package com.download.downloaderbot.bot.gateway.telegram

import com.download.downloaderbot.bot.exception.*
import com.github.kotlintelegrambot.types.TelegramBotResult
import com.download.downloaderbot.bot.gateway.MediaInput
import com.download.downloaderbot.bot.gateway.MessageOptions
import com.download.downloaderbot.core.domain.MediaType
import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.Message
import com.github.kotlintelegrambot.entities.TelegramFile
import com.github.kotlintelegrambot.entities.inputmedia.GroupableMedia
import com.github.kotlintelegrambot.entities.inputmedia.InputMediaPhoto
import com.github.kotlintelegrambot.entities.inputmedia.InputMediaVideo
import com.github.kotlintelegrambot.entities.inputmedia.MediaGroup
import kotlinx.coroutines.delay
import kotlin.collections.toTypedArray
import kotlin.time.Duration.Companion.milliseconds

import com.github.kotlintelegrambot.network.Response as TgEnvelope
import retrofit2.Response as HttpResponse

private const val ALBUM_LIMIT = 10
private const val ALBUM_COOLDOWN_MS = 1_200L

private fun TelegramBotResult.Error.asException(): TelegramBotException = when (this) {
    is TelegramBotResult.Error.HttpError ->
        TelegramHttpException(httpCode, description)
    is TelegramBotResult.Error.TelegramApi ->
        TelegramApiException(errorCode, description)
    is TelegramBotResult.Error.InvalidResponse ->
        TelegramInvalidResponseException(
            httpCode = httpCode,
            httpMessage = httpStatusMessage,
            telegramErrorCode = body?.errorCode,
            telegramErrorDescription = body?.errorDescription
        )
    is TelegramBotResult.Error.Unknown ->
        TelegramUnknownException(exception)
}

fun <T : Any> TelegramBotResult<T>.toResult(): Result<T> = when (this) {
    is TelegramBotResult.Success -> Result.success(value)
    is TelegramBotResult.Error -> Result.failure(asException())
}

fun <T : Any> Pair<HttpResponse<TgEnvelope<T>?>?, Exception?>.toResult(): Result<T> = runCatching {
    val (http, ex) = this

    if (ex != null)
        throw TelegramUnknownException(ex)
    if (http == null)
        throw TelegramUnknownException(IllegalStateException("HTTP response is null"))

    if (!http.isSuccessful) {
        val bodyText = runCatching { http.errorBody()?.string() }
            .getOrNull()
        throw TelegramHttpException(http.code(), bodyText ?: http.message())
    }

    val envelope = http.body()
        ?: throw TelegramInvalidResponseException(
            httpCode = http.code(),
            httpMessage =  http.message(),
            telegramErrorCode = null,
            telegramErrorDescription = null
        )

    if (!envelope.ok) {
        throw TelegramApiException(
            envelope.errorCode ?: -1,
            envelope.errorDescription ?: "Unknown API error"
        )
    }

    envelope.result ?: throw TelegramInvalidResponseException(
        httpCode =  http.code(),
        httpMessage = http.message(),
        telegramErrorCode = envelope.errorCode,
        telegramErrorDescription = envelope.errorDescription
    )
}

fun <T : Any> TelegramBotResult<T>.getOrThrow(): T = toResult().getOrThrow()
fun <T : Any> Pair<HttpResponse<TgEnvelope<T>?>?, Exception?>.getOrThrow(): T = toResult().getOrThrow()

suspend fun Bot.sendSmartMedia(
    type: MediaType,
    chatId: Long,
    file: TelegramFile,
    options: MessageOptions = MessageOptions()
): Message {
    val id = ChatId.fromId(chatId)
    val result = when (type) {
        MediaType.IMAGE -> sendPhoto(chatId = id, photo = file, caption = options.caption, replyToMessageId = options.replyToMessageId, replyMarkup = options.replyMarkup)
        MediaType.VIDEO -> sendVideo(chatId = id, video = file, caption = options.caption, replyToMessageId = options.replyToMessageId, replyMarkup = options.replyMarkup)
        MediaType.AUDIO -> sendAudio(chatId = id, audio = file, title = options.caption, replyToMessageId = options.replyToMessageId, replyMarkup = options.replyMarkup)
    }
    return result.getOrThrow()
}

suspend fun Bot.sendMediaAlbumChunked(
    chatId: Long,
    items: List<MediaInput>,
    caption: String? = null,
    replyToMessageId: Long? = null
): List<Message> {
    val allMessages = mutableListOf<Message>()
    val id = ChatId.fromId(chatId)

    items.chunked(ALBUM_LIMIT).forEachIndexed { idx, part ->
        val groupCaption = caption.takeIf { idx == 0 }

        val mediaArray = part.mapIndexed { itemIdx, item ->
            val itemCaption = groupCaption.takeIf { itemIdx == 0 }
            when (item.type) {
                MediaType.IMAGE -> InputMediaPhoto(media = item.file.toTelegram(), caption = itemCaption)
                MediaType.VIDEO -> InputMediaVideo(media = item.file.toTelegram(), caption = itemCaption)
                MediaType.AUDIO -> error("Audio album is currently not supported")
            }
        }.toTypedArray<GroupableMedia>()

        val messages = sendMediaGroup(
            chatId = id,
            mediaGroup = MediaGroup.from(*mediaArray),
            replyToMessageId = replyToMessageId
        ).getOrThrow()

        allMessages.addAll(messages)

        if (idx < items.chunked(ALBUM_LIMIT).size - 1) {
            delay(ALBUM_COOLDOWN_MS.milliseconds)
        }
    }

    return allMessages
}

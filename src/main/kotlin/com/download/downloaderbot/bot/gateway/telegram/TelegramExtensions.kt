package com.download.downloaderbot.bot.gateway.telegram

import com.download.downloaderbot.bot.commands.CommandContext
import com.download.downloaderbot.bot.exception.TelegramApiException
import com.download.downloaderbot.bot.exception.TelegramBotException
import com.download.downloaderbot.bot.exception.TelegramHttpException
import com.download.downloaderbot.bot.exception.TelegramInvalidResponseException
import com.download.downloaderbot.bot.exception.TelegramUnknownException
import com.download.downloaderbot.core.domain.MediaType
import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.Message
import com.github.kotlintelegrambot.entities.ReplyMarkup
import com.github.kotlintelegrambot.entities.TelegramFile
import com.github.kotlintelegrambot.entities.files.PhotoSize
import com.github.kotlintelegrambot.entities.inputmedia.GroupableMedia
import com.github.kotlintelegrambot.entities.inputmedia.MediaGroup
import com.github.kotlintelegrambot.types.TelegramBotResult
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds
import com.github.kotlintelegrambot.network.Response as TgEnvelope
import retrofit2.Response as HttpResponse

private const val ALBUM_LIMIT = 10
private const val ALBUM_COOLDOWN_MS = 1_200L

private fun TelegramBotResult.Error.asException(): TelegramBotException =
    when (this) {
        is TelegramBotResult.Error.HttpError ->
            TelegramHttpException(httpCode, description)
        is TelegramBotResult.Error.TelegramApi ->
            TelegramApiException(errorCode, description)
        is TelegramBotResult.Error.InvalidResponse ->
            TelegramInvalidResponseException(
                httpCode = httpCode,
                httpMessage = httpStatusMessage,
                telegramErrorCode = body?.errorCode,
                telegramErrorDescription = body?.errorDescription,
            )
        is TelegramBotResult.Error.Unknown ->
            TelegramUnknownException(exception)
    }

fun <T : Any> TelegramBotResult<T>.toResult(): Result<T> =
    when (this) {
        is TelegramBotResult.Success -> Result.success(value)
        is TelegramBotResult.Error -> Result.failure(asException())
    }

fun <T : Any> Pair<HttpResponse<TgEnvelope<T>?>?, Exception?>.toResult(): Result<T> =
    runCatching {
        val (http, ex) = this

        if (ex != null) {
            throw TelegramUnknownException(ex)
        }
        if (http == null) {
            throw TelegramUnknownException(IllegalStateException("HTTP response is null"))
        }

        if (!http.isSuccessful) {
            val bodyText =
                runCatching { http.errorBody()?.string() }
                    .getOrNull()
            throw TelegramHttpException(http.code(), bodyText ?: http.message())
        }

        val envelope =
            http.body()
                ?: throw TelegramInvalidResponseException(
                    httpCode = http.code(),
                    httpMessage = http.message(),
                    telegramErrorCode = null,
                    telegramErrorDescription = null,
                )

        if (!envelope.ok) {
            throw TelegramApiException(
                envelope.errorCode ?: -1,
                envelope.errorDescription ?: "Unknown API error",
            )
        }

        envelope.result ?: throw TelegramInvalidResponseException(
            httpCode = http.code(),
            httpMessage = http.message(),
            telegramErrorCode = envelope.errorCode,
            telegramErrorDescription = envelope.errorDescription,
        )
    }

fun <T : Any> TelegramBotResult<T>.getOrThrow(): T = toResult().getOrThrow()
fun <T : Any> Pair<HttpResponse<TgEnvelope<T>?>?, Exception?>.getOrThrow(): T = toResult().getOrThrow()

suspend fun Bot.sendMedia(
    type: MediaType,
    chatId: Long,
    file: TelegramFile,
    caption: String? = null,
    replyToMessageId: Long? = null,
    replyMarkup: ReplyMarkup? = null,
): Message =
    when (type) {
        MediaType.IMAGE ->
            sendPhoto(
                chatId = ChatId.fromId(chatId),
                photo = file,
                caption = caption,
                replyToMessageId = replyToMessageId,
                replyMarkup = replyMarkup,
            )
        MediaType.VIDEO ->
            sendVideo(
                chatId = ChatId.fromId(chatId),
                video = file,
                caption = caption,
                replyToMessageId = replyToMessageId,
                replyMarkup = replyMarkup,
            )
        MediaType.AUDIO ->
            sendAudio(
                chatId = ChatId.fromId(chatId),
                audio = file,
                title = caption,
                replyToMessageId = replyToMessageId,
                replyMarkup = replyMarkup,
            )
    }.getOrThrow()

suspend fun Bot.sendMediaAlbum(
    chatId: Long,
    items: List<GroupableMedia>,
    replyToMessageId: Long? = null,
): List<Message> {
    val chunks = items.chunked(ALBUM_LIMIT)
    val id = ChatId.fromId(chatId)

    return chunks.flatMapIndexed { index, part ->
        val messages = sendMediaGroup(
            chatId = id,
            mediaGroup = MediaGroup.from(*part.toTypedArray()),
            replyToMessageId = replyToMessageId,
        ).getOrThrow()

        if (index < chunks.lastIndex) {
            delay(ALBUM_COOLDOWN_MS.milliseconds)
        }

        messages
    }
}

val CommandContext.chatId: Long
    get() =
        update.message?.chat?.id
            ?: update.callbackQuery
                ?.message
                ?.chat
                ?.id
            ?: error("No chatId in update")

val CommandContext.replyToMessageId: Long?
    get() =
        update.message?.messageId
            ?: update.callbackQuery?.message?.messageId

val CommandContext.chatType: String
    get() =
        update.message?.chat?.type
            ?: update.callbackQuery
                ?.message
                ?.chat
                ?.type
            ?: error("No chat type in update")

val CommandContext.isPrivateChat: Boolean
    get() = chatType == "private"

val CommandContext.isGroupChat: Boolean
    get() = chatType == "group" || chatType == "supergroup"

val Message.fileId: String?
    get() =
        this.photo.largest()?.fileId
            ?: this.document?.fileId
            ?: this.video?.fileId
            ?: this.audio?.fileId
            ?: this.animation?.fileId

val Message.fileUniqueId: String?
    get() =
        this.photo.largest()?.fileUniqueId
            ?: this.document?.fileUniqueId
            ?: this.video?.fileUniqueId
            ?: this.audio?.fileUniqueId
            ?: this.animation?.fileUniqueId

private fun List<PhotoSize>?.largest(): PhotoSize? =
    this?.maxByOrNull { it.width * it.height }

package com.download.downloaderbot.bot.gateway

import com.download.downloaderbot.core.domain.MediaType
import com.github.kotlintelegrambot.entities.Message
import com.github.kotlintelegrambot.entities.ReplyMarkup
import kotlinx.coroutines.delay

private const val ALBUM_LIMIT = 10
private const val ALBUM_COOLDOWN_MS = 1200L

data class MessageOptions(
    val caption: String? = null,
    val replyToMessageId: Long? = null,
    val replyMarkup: ReplyMarkup? = null,
)

data class VideoOptions(
    val durationSeconds: Int? = null,
    val width: Int? = null,
    val height: Int? = null,
)

data class AudioOptions(
    val durationSeconds: Int? = null,
    val performer: String? = null,
    val title: String? = null,
)

interface BotPort {
    suspend fun sendText(
        chatId: Long,
        text: String,
        replyToMessageId: Long? = null,
        replyMarkup: ReplyMarkup? = null,
    ): GatewayResult<Message>

    suspend fun sendMedia(
        type: MediaType,
        chatId: Long,
        file: InputFile,
        options: MessageOptions = MessageOptions(),
    ): GatewayResult<Message> =
        when (type) {
            MediaType.IMAGE -> sendPhoto(chatId, file, options)
            MediaType.VIDEO ->
                sendVideo(
                    chatId,
                    file,
                    options,
                )
            MediaType.AUDIO ->
                sendAudio(
                    chatId,
                    file,
                    options,
                    AudioOptions(title = options.caption),
                )
        }

    suspend fun sendPhoto(
        chatId: Long,
        file: InputFile,
        options: MessageOptions = MessageOptions(),
    ): GatewayResult<Message>

    suspend fun sendVideo(
        chatId: Long,
        file: InputFile,
        messageOptions: MessageOptions = MessageOptions(),
        videoOptions: VideoOptions = VideoOptions(),
    ): GatewayResult<Message>

    suspend fun sendAudio(
        chatId: Long,
        file: InputFile,
        messageOptions: MessageOptions = MessageOptions(),
        audioOptions: AudioOptions = AudioOptions(),
    ): GatewayResult<Message>

    suspend fun sendDocument(
        chatId: Long,
        file: InputFile,
        options: MessageOptions = MessageOptions(),
    ): GatewayResult<Message>

    suspend fun sendPhotoAlbum(
        chatId: Long,
        files: List<InputFile>,
        caption: String? = null,
        replyToMessageId: Long? = null,
    ): GatewayResult<List<Message>>

    suspend fun sendPhotoAlbumChunked(
        chatId: Long,
        files: List<InputFile>,
        chunk: Int = ALBUM_LIMIT,
        caption: String? = null,
        replyToMessageId: Long? = null,
    ): GatewayResult<List<Message>> {
        val all = mutableListOf<Message>()
        for ((idx, part) in files.chunked(chunk).withIndex()) {
            val cap = caption.takeIf { idx == 0 }
            when (val res = sendPhotoAlbum(chatId, part, cap, replyToMessageId)) {
                is GatewayResult.Ok -> {
                    all += res.value
                    delay(ALBUM_COOLDOWN_MS) // heuristic delay to avoid Telegram limits
                }
                is GatewayResult.Err -> return res
            }
        }
        return GatewayResult.Ok(all)
    }
}

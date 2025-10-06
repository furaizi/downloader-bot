package com.download.downloaderbot.bot.gateway

import com.download.downloaderbot.core.domain.MediaType
import com.github.kotlintelegrambot.entities.Message
import com.github.kotlintelegrambot.entities.ReplyMarkup

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
        caption: String? = null,
        replyToMessageId: Long? = null,
        replyMarkup: ReplyMarkup? = null,
    ): GatewayResult<Message> =
        when (type) {
            MediaType.IMAGE -> sendPhoto(chatId, file, caption, replyToMessageId, replyMarkup)
            MediaType.VIDEO ->
                sendVideo(
                    chatId,
                    file,
                    caption,
                    replyToMessageId = replyToMessageId,
                    replyMarkup = replyMarkup,
                )
            MediaType.AUDIO ->
                sendAudio(
                    chatId,
                    file,
                    title = caption,
                    replyToMessageId = replyToMessageId,
                    replyMarkup = replyMarkup,
                )
        }

    suspend fun sendPhoto(
        chatId: Long,
        file: InputFile,
        caption: String? = null,
        replyToMessageId: Long? = null,
        replyMarkup: ReplyMarkup? = null,
    ): GatewayResult<Message>

    suspend fun sendVideo(
        chatId: Long,
        file: InputFile,
        caption: String? = null,
        durationSeconds: Int? = null,
        width: Int? = null,
        height: Int? = null,
        replyToMessageId: Long? = null,
        replyMarkup: ReplyMarkup? = null,
    ): GatewayResult<Message>

    suspend fun sendAudio(
        chatId: Long,
        file: InputFile,
        durationSeconds: Int? = null,
        performer: String? = null,
        title: String? = null,
        replyToMessageId: Long? = null,
        replyMarkup: ReplyMarkup? = null,
    ): GatewayResult<Message>

    suspend fun sendDocument(
        chatId: Long,
        file: InputFile,
        caption: String? = null,
        replyToMessageId: Long? = null,
        replyMarkup: ReplyMarkup? = null,
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
        chunk: Int = 10,
        caption: String? = null,
        replyToMessageId: Long? = null,
    ): GatewayResult<List<Message>> {
        val all = mutableListOf<Message>()
        for ((idx, part) in files.chunked(chunk).withIndex()) {
            val cap = caption.takeIf { idx == 0 }
            when (val res = sendPhotoAlbum(chatId, part, cap, replyToMessageId)) {
                is GatewayResult.Ok -> all += res.value
                is GatewayResult.Err -> return res
            }
        }
        return GatewayResult.Ok(all)
    }
}

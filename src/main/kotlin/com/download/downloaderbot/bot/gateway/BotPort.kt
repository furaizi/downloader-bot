package com.download.downloaderbot.bot.gateway

import com.github.kotlintelegrambot.entities.Message
import java.io.File

interface BotPort {
    
    suspend fun sendText(
        chatId: Long,
        text: String,
        replyToMessageId: Long? = null
    ): GatewayResult<Message>

    suspend fun sendPhoto(
        chatId: Long,
        file: File,
        caption: String? = null,
        replyToMessageId: Long? = null
    ): GatewayResult<Message>

    suspend fun sendVideo(
        chatId: Long,
        file: File,
        caption: String? = null,
        durationSeconds: Int? = null,
        width: Int? = null,
        height: Int? = null,
        replyToMessageId: Long? = null
    ): GatewayResult<Message>

    suspend fun sendAudio(
        chatId: Long,
        file: File,
        durationSeconds: Int? = null,
        performer: String? = null,
        title: String? = null,
        replyToMessageId: Long? = null
    ): GatewayResult<Message>

    suspend fun sendDocument(
        chatId: Long,
        file: File,
        caption: String? = null,
        replyToMessageId: Long? = null
    ): GatewayResult<Message>

    suspend fun sendPhotoAlbum(
        chatId: Long,
        files: List<File>,
        caption: String? = null,
        replyToMessageId: Long? = null
    ): GatewayResult<List<Message>>

    suspend fun sendPhotoAlbumChunked(
        chatId: Long,
        files: List<File>,
        chunk: Int = 10,
        caption: String? = null,
        replyToMessageId: Long? = null
    ): GatewayResult<List<Message>> {
        val all = mutableListOf<Message>()

        for ((idx, part) in files.chunked(chunk).withIndex()) {
            val cap = caption.takeIf { idx == 0 }
            when (val res = sendPhotoAlbum(chatId, part, cap, replyToMessageId)) {
                is GatewayResult.Ok -> all += res.value
                is GatewayResult.Err   -> return res
            }
        }

        return GatewayResult.Ok(all)
    }
}
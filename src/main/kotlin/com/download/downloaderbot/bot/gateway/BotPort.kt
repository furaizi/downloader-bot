package com.download.downloaderbot.bot.gateway

import java.io.File

interface BotPort {
    
    suspend fun sendText(
        chatId: Long,
        text: String,
        replyToMessageId: Long? = null
    )

    suspend fun sendPhoto(
        chatId: Long,
        file: File,
        caption: String? = null,
        replyToMessageId: Long? = null
    )

    suspend fun sendVideo(
        chatId: Long,
        file: File,
        caption: String? = null,
        durationSeconds: Int? = null,
        width: Int? = null,
        height: Int? = null,
        replyToMessageId: Long? = null
    )

    suspend fun sendAudio(
        chatId: Long,
        file: File,
        durationSeconds: Int? = null,
        performer: String? = null,
        title: String? = null,
        replyToMessageId: Long? = null
    )

    suspend fun sendDocument(
        chatId: Long,
        file: File,
        caption: String? = null,
        replyToMessageId: Long? = null
    )

    suspend fun sendPhotoAlbum(
        chatId: Long,
        files: List<File>,
        caption: String? = null,
        replyToMessageId: Long? = null
    )

    suspend fun sendPhotoAlbumChunked(
        chatId: Long,
        files: List<File>,
        chunk: Int = 10,
        caption: String? = null,
        replyToMessageId: Long? = null
    ) {
        files.chunked(chunk).forEachIndexed { idx, part ->
            val cap = caption.takeIf { idx == 0 }
            sendPhotoAlbum(chatId, part, cap, replyToMessageId)
        }
    }
}
package com.download.downloaderbot.bot.gateway

import com.github.kotlintelegrambot.entities.Message
import java.io.File

sealed class InputFile {
    data class Local(val file: File) : InputFile()
    data class Id(val fileId: String) : InputFile()
}

interface BotPort {

    suspend fun sendText(
        chatId: Long,
        text: String,
        replyToMessageId: Long? = null
    ): GatewayResult<Message>

    suspend fun sendPhoto(
        chatId: Long,
        file: InputFile,
        caption: String? = null,
        replyToMessageId: Long? = null
    ): GatewayResult<Message>

    suspend fun sendPhoto(
        chatId: Long,
        file: File,
        caption: String? = null,
        replyToMessageId: Long? = null
    ): GatewayResult<Message> = sendPhoto(chatId, InputFile.Local(file), caption, replyToMessageId)

    suspend fun sendPhoto(
        chatId: Long,
        fileId: String,
        caption: String? = null,
        replyToMessageId: Long? = null
    ): GatewayResult<Message> = sendPhoto(chatId, InputFile.Id(fileId), caption, replyToMessageId)

    suspend fun sendVideo(
        chatId: Long,
        file: InputFile,
        caption: String? = null,
        durationSeconds: Int? = null,
        width: Int? = null,
        height: Int? = null,
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
    ): GatewayResult<Message> = sendVideo(
        chatId, InputFile.Local(file), caption, durationSeconds, width, height, replyToMessageId
    )

    suspend fun sendVideo(
        chatId: Long,
        fileId: String,
        caption: String? = null,
        durationSeconds: Int? = null,
        width: Int? = null,
        height: Int? = null,
        replyToMessageId: Long? = null
    ): GatewayResult<Message> = sendVideo(
        chatId, InputFile.Id(fileId), caption, durationSeconds, width, height, replyToMessageId
    )

    suspend fun sendAudio(
        chatId: Long,
        file: InputFile,
        durationSeconds: Int? = null,
        performer: String? = null,
        title: String? = null,
        replyToMessageId: Long? = null
    ): GatewayResult<Message>

    suspend fun sendAudio(
        chatId: Long,
        file: File,
        durationSeconds: Int? = null,
        performer: String? = null,
        title: String? = null,
        replyToMessageId: Long? = null
    ): GatewayResult<Message> = sendAudio(
        chatId, InputFile.Local(file), durationSeconds, performer, title, replyToMessageId
    )

    suspend fun sendAudio(
        chatId: Long,
        fileId: String,
        durationSeconds: Int? = null,
        performer: String? = null,
        title: String? = null,
        replyToMessageId: Long? = null
    ): GatewayResult<Message> = sendAudio(
        chatId, InputFile.Id(fileId), durationSeconds, performer, title, replyToMessageId
    )

    suspend fun sendDocument(
        chatId: Long,
        file: InputFile,
        caption: String? = null,
        replyToMessageId: Long? = null
    ): GatewayResult<Message>

    suspend fun sendDocument(
        chatId: Long,
        file: File,
        caption: String? = null,
        replyToMessageId: Long? = null
    ): GatewayResult<Message> = sendDocument(chatId, InputFile.Local(file), caption, replyToMessageId)

    suspend fun sendDocument(
        chatId: Long,
        fileId: String,
        caption: String? = null,
        replyToMessageId: Long? = null
    ): GatewayResult<Message> = sendDocument(chatId, InputFile.Id(fileId), caption, replyToMessageId)

    suspend fun sendPhotoAlbum(
        chatId: Long,
        files: List<InputFile>,
        caption: String? = null,
        replyToMessageId: Long? = null
    ): GatewayResult<List<Message>>

    suspend fun sendPhotoAlbumFiles(
        chatId: Long,
        files: List<File>,
        caption: String? = null,
        replyToMessageId: Long? = null
    ): GatewayResult<List<Message>> = sendPhotoAlbum(
        chatId, files.map { InputFile.Local(it) }, caption, replyToMessageId
    )

    suspend fun sendPhotoAlbumIds(
        chatId: Long,
        fileIds: List<String>,
        caption: String? = null,
        replyToMessageId: Long? = null
    ): GatewayResult<List<Message>> = sendPhotoAlbum(
        chatId, fileIds.map { InputFile.Id(it) }, caption, replyToMessageId
    )

    suspend fun sendPhotoAlbumChunked(
        chatId: Long,
        files: List<InputFile>,
        chunk: Int = 10,
        caption: String? = null,
        replyToMessageId: Long? = null
    ): GatewayResult<List<Message>> {
        val all = mutableListOf<Message>()
        for ((idx, part) in files.chunked(chunk).withIndex()) {
            val cap = caption.takeIf { idx == 0 }
            when (val res = sendPhotoAlbum(chatId, part, cap, replyToMessageId)) {
                is GatewayResult.Ok  -> all += res.value
                is GatewayResult.Err -> return res
            }
        }
        return GatewayResult.Ok(all)
    }

    suspend fun sendPhotoAlbumChunkedFiles(
        chatId: Long,
        files: List<File>,
        chunk: Int = 10,
        caption: String? = null,
        replyToMessageId: Long? = null
    ): GatewayResult<List<Message>> = sendPhotoAlbumChunked(
        chatId, files.map { InputFile.Local(it) }, chunk, caption, replyToMessageId
    )

    suspend fun sendPhotoAlbumChunkedIds(
        chatId: Long,
        fileIds: List<String>,
        chunk: Int = 10,
        caption: String? = null,
        replyToMessageId: Long? = null
    ): GatewayResult<List<Message>> = sendPhotoAlbumChunked(
        chatId, fileIds.map { InputFile.Id(it) }, chunk, caption, replyToMessageId
    )
}

package com.download.downloaderbot.bot.gateway

import com.download.downloaderbot.core.domain.MediaType
import com.github.kotlintelegrambot.entities.Message
import com.github.kotlintelegrambot.entities.ReplyMarkup
import com.github.kotlintelegrambot.entities.files.PhotoSize
import io.mockk.every
import io.mockk.mockk
import java.util.concurrent.CopyOnWriteArrayList

class RecordingBotPort : BotPort {
    data class SentText(
        val chatId: Long,
        val text: String,
        val replyToMessageId: Long?,
        val replyMarkup: ReplyMarkup?,
        val message: Message,
    )

    data class SentMedia(
        val type: MediaType,
        val chatId: Long,
        val file: InputFile,
        val options: MessageOptions,
        val message: Message,
    )

    data class SentAlbum(
        val chatId: Long,
        val files: List<InputFile>,
        val caption: String?,
        val replyToMessageId: Long?,
        val messages: List<Message>,
    )

    data class SentChunkedAlbum(
        val chatId: Long,
        val files: List<InputFile>,
        val chunkSize: Int,
        val caption: String?,
        val replyToMessageId: Long?,
        val messages: List<Message>,
    )

    val sentTexts = CopyOnWriteArrayList<SentText>()
    val sentMedia = CopyOnWriteArrayList<SentMedia>()
    val sentAlbums = CopyOnWriteArrayList<SentAlbum>()
    val sentChunkedAlbums = CopyOnWriteArrayList<SentChunkedAlbum>()

    private var nextId = 1L

    fun reset() {
        sentTexts.clear()
        sentMedia.clear()
        sentAlbums.clear()
        sentChunkedAlbums.clear()
        nextId = 1L
    }

    override suspend fun sendText(
        chatId: Long,
        text: String,
        replyToMessageId: Long?,
        replyMarkup: ReplyMarkup?,
    ): GatewayResult<Message> {
        val message = newMessage()
        sentTexts += SentText(chatId, text, replyToMessageId, replyMarkup, message)
        return GatewayResult.Ok(message)
    }

    override suspend fun sendPhoto(
        chatId: Long,
        file: InputFile,
        options: MessageOptions,
    ): GatewayResult<Message> {
        val message = recordMedia(MediaType.IMAGE, chatId, file, options)
        return GatewayResult.Ok(message)
    }

    override suspend fun sendVideo(
        chatId: Long,
        file: InputFile,
        messageOptions: MessageOptions,
        videoOptions: VideoOptions,
    ): GatewayResult<Message> {
        val message = recordMedia(MediaType.VIDEO, chatId, file, messageOptions)
        return GatewayResult.Ok(message)
    }

    override suspend fun sendAudio(
        chatId: Long,
        file: InputFile,
        messageOptions: MessageOptions,
        audioOptions: AudioOptions,
    ): GatewayResult<Message> {
        val message = recordMedia(MediaType.AUDIO, chatId, file, messageOptions)
        return GatewayResult.Ok(message)
    }

    override suspend fun sendDocument(
        chatId: Long,
        file: InputFile,
        options: MessageOptions,
    ): GatewayResult<Message> {
        val message = recordMedia(MediaType.VIDEO, chatId, file, options)
        return GatewayResult.Ok(message)
    }

    override suspend fun sendMediaAlbum(
        chatId: Long,
        media: List<InputFile>,
        caption: String?,
        replyToMessageId: Long?,
    ): GatewayResult<List<Message>> {
        val messages = media.map { newMessage() }
        sentAlbums += SentAlbum(chatId, media, caption, replyToMessageId, messages)
        return GatewayResult.Ok(messages)
    }

    override suspend fun sendPhotoAlbumChunked(
        chatId: Long,
        files: List<InputFile>,
        chunk: Int,
        caption: String?,
        replyToMessageId: Long?,
    ): GatewayResult<List<Message>> {
        val messages = files.map { newMessage() }
        sentChunkedAlbums += SentChunkedAlbum(chatId, files, chunk, caption, replyToMessageId, messages)
        return GatewayResult.Ok(messages)
    }

    private fun recordMedia(
        type: MediaType,
        chatId: Long,
        file: InputFile,
        options: MessageOptions,
    ): Message {
        val message = newMessage()
        sentMedia += SentMedia(type, chatId, file, options, message)
        return message
    }

    private fun newMessage(): Message {
        val id = nextId++
        val message = mockk<Message>()
        val photo = mockk<PhotoSize>()

        every { photo.width } returns 100
        every { photo.height } returns 100
        every { photo.fileId } returns "file_$id"
        every { photo.fileUniqueId } returns "unique_$id"

        every { message.photo } returns listOf(photo)
        every { message.messageId } returns id

        return message
    }
}

package com.download.downloaderbot.bot.gateway

import com.download.downloaderbot.core.domain.MediaType
import com.github.kotlintelegrambot.entities.Message
import com.github.kotlintelegrambot.entities.ReplyMarkup
import com.github.kotlintelegrambot.entities.files.Audio
import com.github.kotlintelegrambot.entities.files.Document
import com.github.kotlintelegrambot.entities.files.PhotoSize
import com.github.kotlintelegrambot.entities.files.Video
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

    data class SentDocument(
        val chatId: Long,
        val file: InputFile,
        val options: MessageOptions,
        val message: Message,
    )

    data class SentAlbum(
        val chatId: Long,
        val items: List<MediaInput>,
        val caption: String?,
        val replyToMessageId: Long?,
        val messages: List<Message>,
    )

    data class SentChunkedAlbum(
        val chatId: Long,
        val items: List<MediaInput>,
        val chunkSize: Int,
        val caption: String?,
        val replyToMessageId: Long?,
        val messages: List<Message>,
    )

    val sentTexts = CopyOnWriteArrayList<SentText>()
    val sentMedia = CopyOnWriteArrayList<SentMedia>()
    val sentDocuments = CopyOnWriteArrayList<SentDocument>()
    val sentAlbums = CopyOnWriteArrayList<SentAlbum>()
    val sentChunkedAlbums = CopyOnWriteArrayList<SentChunkedAlbum>()

    private var nextId = 1L

    fun reset() {
        sentTexts.clear()
        sentMedia.clear()
        sentDocuments.clear()
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
    ): GatewayResult<Message> = GatewayResult.Ok(recordMedia(MediaType.IMAGE, chatId, file, options))

    override suspend fun sendVideo(
        chatId: Long,
        file: InputFile,
        messageOptions: MessageOptions,
        videoOptions: VideoOptions,
    ): GatewayResult<Message> = GatewayResult.Ok(recordMedia(MediaType.VIDEO, chatId, file, messageOptions))

    override suspend fun sendAudio(
        chatId: Long,
        file: InputFile,
        messageOptions: MessageOptions,
        audioOptions: AudioOptions,
    ): GatewayResult<Message> = GatewayResult.Ok(recordMedia(MediaType.AUDIO, chatId, file, messageOptions))

    override suspend fun sendDocument(
        chatId: Long,
        file: InputFile,
        options: MessageOptions,
    ): GatewayResult<Message> = GatewayResult.Ok(recordDocument(chatId, file, options))

    override suspend fun sendMediaAlbum(
        chatId: Long,
        items: List<MediaInput>,
        caption: String?,
        replyToMessageId: Long?,
    ): GatewayResult<List<Message>> {
        val messages = items.map { newTypedMessage(it.type) }
        sentAlbums += SentAlbum(chatId, items, caption, replyToMessageId, messages)
        return GatewayResult.Ok(messages)
    }

    override suspend fun sendMediaAlbumChunked(
        chatId: Long,
        items: List<MediaInput>,
        chunk: Int,
        caption: String?,
        replyToMessageId: Long?,
    ): GatewayResult<List<Message>> {
        val messages = items.map { newTypedMessage(it.type) }
        sentChunkedAlbums += SentChunkedAlbum(chatId, items, chunk, caption, replyToMessageId, messages)
        return GatewayResult.Ok(messages)
    }

    private fun recordMedia(
        type: MediaType,
        chatId: Long,
        file: InputFile,
        options: MessageOptions,
    ): Message {
        val message = newTypedMessage(type)
        sentMedia += SentMedia(type, chatId, file, options, message)
        return message
    }

    private fun recordDocument(
        chatId: Long,
        file: InputFile,
        options: MessageOptions,
    ): Message {
        val message = newTypedMessage(type = null, attachDocument = true)
        sentDocuments += SentDocument(chatId, file, options, message)
        return message
    }

    private fun newMessage(): Message = newTypedMessage(type = null)

    private fun newTypedMessage(
        type: MediaType?,
        attachDocument: Boolean = false,
    ): Message {
        val id = nextId++
        val fileId = "file_$id"
        val uniqueId = "unique_$id"

        val message = mockk<Message>(relaxed = true)
        every { message.messageId } returns id

        when (type) {
            MediaType.IMAGE -> every { message.photo } returns listOf(mockPhoto(fileId, uniqueId))
            MediaType.VIDEO -> every { message.video } returns mockVideo(fileId, uniqueId)
            MediaType.AUDIO -> every { message.audio } returns mockAudio(fileId, uniqueId)
            null -> Unit
        }

        if (attachDocument) {
            every { message.document } returns mockDocument(fileId, uniqueId, "doc_$id")
        }

        return message
    }

    private fun mockPhoto(
        fileId: String,
        uniqueId: String,
    ): PhotoSize =
        mockk<PhotoSize>().also { photo ->
            every { photo.width } returns 100
            every { photo.height } returns 100
            every { photo.fileId } returns fileId
            every { photo.fileUniqueId } returns uniqueId
        }

    private fun mockVideo(
        fileId: String,
        uniqueId: String,
    ): Video =
        mockk<Video>().also { video ->
            every { video.fileId } returns fileId
            every { video.fileUniqueId } returns uniqueId
        }

    private fun mockAudio(
        fileId: String,
        uniqueId: String,
    ): Audio =
        mockk<Audio>().also { audio ->
            every { audio.fileId } returns fileId
            every { audio.fileUniqueId } returns uniqueId
        }

    private fun mockDocument(
        fileId: String,
        uniqueId: String,
        fileName: String,
    ): Document =
        mockk<Document>().also { doc ->
            every { doc.fileId } returns fileId
            every { doc.fileUniqueId } returns uniqueId
            every { doc.fileName } returns fileName
        }
}

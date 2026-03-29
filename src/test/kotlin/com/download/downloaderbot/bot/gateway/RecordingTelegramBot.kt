package com.download.downloaderbot.bot.gateway

import com.download.downloaderbot.core.domain.MediaType
import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.Chat
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.Message
import com.github.kotlintelegrambot.entities.ReplyMarkup
import com.github.kotlintelegrambot.entities.TelegramFile
import com.github.kotlintelegrambot.entities.User
import com.github.kotlintelegrambot.entities.files.Audio
import com.github.kotlintelegrambot.entities.files.Document
import com.github.kotlintelegrambot.entities.files.PhotoSize
import com.github.kotlintelegrambot.entities.files.Video
import com.github.kotlintelegrambot.entities.inputmedia.GroupableMedia
import com.github.kotlintelegrambot.entities.inputmedia.InputMedia
import com.github.kotlintelegrambot.entities.inputmedia.InputMediaAudio
import com.github.kotlintelegrambot.entities.inputmedia.InputMediaDocument
import com.github.kotlintelegrambot.entities.inputmedia.InputMediaPhoto
import com.github.kotlintelegrambot.entities.inputmedia.InputMediaVideo
import com.github.kotlintelegrambot.entities.inputmedia.MediaGroup
import com.github.kotlintelegrambot.types.TelegramBotResult
import io.mockk.every
import io.mockk.mockk
import java.util.concurrent.CopyOnWriteArrayList
import com.github.kotlintelegrambot.network.Response as TgEnvelope
import retrofit2.Response as HttpResponse

class RecordingTelegramBot(
    username: String = "test-bot",
) {
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
        val file: TelegramFile,
        val caption: String?,
        val replyToMessageId: Long?,
        val replyMarkup: ReplyMarkup?,
        val message: Message,
    )

    data class SentAlbum(
        val chatId: Long,
        val items: List<GroupableMedia>,
        val caption: String?,
        val replyToMessageId: Long?,
        val messages: List<Message>,
    )

    data class SentChunkedAlbum(
        val chatId: Long,
        val items: List<GroupableMedia>,
        val chunkSize: Int,
        val caption: String?,
        val replyToMessageId: Long?,
        val messages: List<Message>,
    )

    val sentTexts = CopyOnWriteArrayList<SentText>()
    val sentMedia = CopyOnWriteArrayList<SentMedia>()
    val sentAlbums = CopyOnWriteArrayList<SentAlbum>()
    val sentChunkedAlbums = CopyOnWriteArrayList<SentChunkedAlbum>()

    val bot: Bot = mockk(relaxed = true)

    private var nextId = 1L

    init {
        val me =
            User(
                id = 0L,
                isBot = true,
                firstName = "t",
                username = username,
            )

        every { bot.getMe() } returns TelegramBotResult.Success(me)
        every { bot.deleteWebhook(any()) } returns okResponse(true)
        every { bot.startPolling() } returns Unit
        every { bot.stopPolling() } returns Unit

        every {
            bot.sendMessage(any(), any(), any(), any(), any(), any(), any(), any(), any(), any())
        } answers {
            val chatId = firstArg<ChatId>().requireId()
            val message = newMessage(chatId)
            sentTexts +=
                SentText(
                    chatId = chatId,
                    text = secondArg(),
                    replyToMessageId = arg(6),
                    replyMarkup = arg(8),
                    message = message,
                )
            TelegramBotResult.Success(message)
        }

        every {
            bot.sendPhoto(any(), any<TelegramFile>(), any(), any(), any(), any(), any(), any(), any())
        } answers {
            okResponse(
                recordMedia(
                    type = MediaType.IMAGE,
                    chatId = firstArg<ChatId>().requireId(),
                    file = secondArg(),
                    caption = arg(2),
                    replyToMessageId = arg(6),
                    replyMarkup = arg(8),
                ),
            )
        }

        every {
            bot.sendVideo(any(), any<TelegramFile>(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any())
        } answers {
            okResponse(
                recordMedia(
                    type = MediaType.VIDEO,
                    chatId = firstArg<ChatId>().requireId(),
                    file = secondArg(),
                    caption = arg(5),
                    replyToMessageId = arg(9),
                    replyMarkup = arg(11),
                ),
            )
        }

        every {
            bot.sendAudio(any(), any<TelegramFile>(), any(), any(), any(), any(), any(), any(), any(), any())
        } answers {
            okResponse(
                recordMedia(
                    type = MediaType.AUDIO,
                    chatId = firstArg<ChatId>().requireId(),
                    file = secondArg(),
                    caption = arg(4),
                    replyToMessageId = arg(7),
                    replyMarkup = arg(9),
                ),
            )
        }

        every {
            bot.sendMediaGroup(any(), any(), any(), any(), any(), any())
        } answers {
            val chatId = firstArg<ChatId>().requireId()
            val items = secondArg<MediaGroup>().medias.toList()
            val messages = items.map { item -> newTypedMessage(mediaTypeOf(item), chatId, item is InputMediaDocument) }

            sentAlbums +=
                SentAlbum(
                    chatId = chatId,
                    items = items,
                    caption = items.filterIsInstance<InputMedia>().firstOrNull { it.caption != null }?.caption,
                    replyToMessageId = arg(4),
                    messages = messages,
                )

            TelegramBotResult.Success(messages)
        }
    }

    fun reset() {
        sentTexts.clear()
        sentMedia.clear()
        sentAlbums.clear()
        sentChunkedAlbums.clear()
        nextId = 1L
    }

    private fun recordMedia(
        type: MediaType,
        chatId: Long,
        file: TelegramFile,
        caption: String?,
        replyToMessageId: Long?,
        replyMarkup: ReplyMarkup?,
    ): Message {
        val message = newTypedMessage(type, chatId)
        sentMedia +=
            SentMedia(
                type = type,
                chatId = chatId,
                file = file,
                caption = caption,
                replyToMessageId = replyToMessageId,
                replyMarkup = replyMarkup,
                message = message,
            )
        return message
    }

    private fun newMessage(chatId: Long): Message = newTypedMessage(type = null, chatId = chatId)

    private fun newTypedMessage(
        type: MediaType?,
        chatId: Long,
        attachDocument: Boolean = false,
    ): Message {
        val id = nextId++
        val fileId = "file_$id"
        val uniqueId = "unique_$id"

        return Message(
            messageId = id,
            date = 0L,
            chat = Chat(chatId, "private"),
            photo = if (type == MediaType.IMAGE) listOf(mockPhoto(fileId, uniqueId)) else null,
            video = if (type == MediaType.VIDEO) mockVideo(fileId, uniqueId) else null,
            audio = if (type == MediaType.AUDIO) mockAudio(fileId, uniqueId) else null,
            document = if (attachDocument) mockDocument(fileId, uniqueId, "doc_$id") else null,
        )
    }

    private fun mediaTypeOf(item: GroupableMedia): MediaType? =
        when (item) {
            is InputMediaPhoto -> MediaType.IMAGE
            is InputMediaVideo -> MediaType.VIDEO
            is InputMediaAudio -> MediaType.AUDIO
            is InputMediaDocument -> null
            else -> null
        }

    private fun ChatId.requireId(): Long =
        when (this) {
            is ChatId.Id -> id
            is ChatId.ChannelUsername -> error("Channel usernames are not supported in RecordingTelegramBot")
        }

    private fun <T> okResponse(result: T): Pair<HttpResponse<TgEnvelope<T>?>?, Exception?> =
        HttpResponse.success(TgEnvelope(result, true)) to null

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

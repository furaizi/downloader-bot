package com.download.downloaderbot.bot.gateway

import com.download.downloaderbot.bot.commands.CommandContext
import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.TelegramFile
import com.github.kotlintelegrambot.entities.inputmedia.InputMediaPhoto
import com.github.kotlintelegrambot.entities.inputmedia.MediaGroup
import org.springframework.beans.factory.ObjectProvider
import org.springframework.stereotype.Component
import java.io.File
import java.nio.file.Path

@Component
class TelegramGateway(private val botProvider: ObjectProvider<Bot>) {

    private val bot: Bot by lazy { botProvider.getObject() }

    suspend fun replyText(chatId: Long, text: String, replyToMessageId: Long? = null) {
        bot.sendMessage(
            chatId = ChatId.fromId(chatId),
            text = text,
            replyToMessageId = replyToMessageId
        )
    }

    suspend fun sendVideo(
        chatId: Long,
        file: File,
        caption: String? = null,
        durationSeconds: Int? = null,
        width: Int? = null,
        height: Int? = null,
        replyToMessageId: Long? = null
    ) {
        bot.sendVideo(
            chatId = ChatId.fromId(chatId),
            video = TelegramFile.ByFile(file),
            caption = caption,
            duration = durationSeconds,
            width = width,
            height = height,
            replyToMessageId = replyToMessageId
        )
    }

    suspend fun sendVideo(
        chatId: Long,
        path: Path,
        caption: String? = null,
        durationSeconds: Int? = null,
        width: Int? = null,
        height: Int? = null,
        replyToMessageId: Long? = null
    ) = sendVideo(chatId, path.toFile(), caption, durationSeconds, width, height, replyToMessageId)

    suspend fun sendPhoto(
        chatId: Long,
        file: File,
        caption: String? = null,
        replyToMessageId: Long? = null
    ) {
        bot.sendPhoto(
            chatId = ChatId.fromId(chatId),
            photo = TelegramFile.ByFile(file),
            caption = caption,
            replyToMessageId = replyToMessageId
        )
    }

    suspend fun sendPhoto(
        chatId: Long,
        path: Path,
        caption: String? = null,
        replyToMessageId: Long? = null
    ) = sendPhoto(chatId, path.toFile(), caption, replyToMessageId)


    suspend fun sendPhotosAlbum(
        chatId: Long,
        files: List<File>,
        caption: String? = null,
        replyToMessageId: Long? = null
    ) {
        val media = files.mapIndexed { index, file ->
            InputMediaPhoto(
                media = TelegramFile.ByFile(file),
                caption = caption.takeIf { index == 0 }
            )
        }.toTypedArray()

        bot.sendMediaGroup(
            chatId = ChatId.fromId(chatId),
            mediaGroup = MediaGroup.from(*media),
            replyToMessageId = replyToMessageId
        )
    }

    suspend fun sendPhotosAlbumChunked(
        chatId: Long,
        files: List<File>,
        caption: String? = null,
        replyToMessageId: Long? = null
    ) {
        files.chunked(10).forEachIndexed { idx, chunk ->
            val shouldCaption = idx == 0
            sendPhotosAlbum(
                chatId = chatId,
                files = chunk,
                caption = caption.takeIf { shouldCaption },
                replyToMessageId = replyToMessageId
            )
        }
    }

    // Compiler doesn't allow to have both List<File> and List<Path> overloads
    // due to type erasure
    suspend fun sendPhotosAlbumFromPaths(
        chatId: Long,
        paths: List<Path>,
        caption: String? = null,
        replyToMessageId: Long? = null
    ) = sendPhotosAlbum(chatId, paths.map { it.toFile() }, caption, replyToMessageId)

    suspend fun sendPhotosAlbumChunkedFromPaths(
        chatId: Long,
        paths: List<Path>,
        caption: String? = null,
        replyToMessageId: Long? = null
    ) = sendPhotosAlbumChunked(chatId, paths.map { it.toFile() }, caption, replyToMessageId)

    suspend fun sendAudio(
        chatId: Long,
        file: File,
        durationSeconds: Int? = null,
        performer: String? = null,
        title: String? = null,
        replyToMessageId: Long? = null
    ) {
        bot.sendAudio(
            chatId = ChatId.fromId(chatId),
            audio = TelegramFile.ByFile(file),
            duration = durationSeconds,
            performer = performer,
            title = title,
            replyToMessageId = replyToMessageId
        )
    }

    suspend fun sendAudio(
        chatId: Long,
        path: Path,
        durationSeconds: Int? = null,
        performer: String? = null,
        title: String? = null,
        replyToMessageId: Long? = null
    ) = sendAudio(chatId, path.toFile(), durationSeconds, performer, title, replyToMessageId)

    suspend fun sendDocument(
        chatId: Long,
        file: File,
        caption: String? = null,
        replyToMessageId: Long? = null
    ) {
        bot.sendDocument(
            chatId = ChatId.fromId(chatId),
            document = TelegramFile.ByFile(file),
            caption = caption,
            replyToMessageId = replyToMessageId
        )
    }

    suspend fun sendDocument(
        chatId: Long,
        path: Path,
        caption: String? = null,
        replyToMessageId: Long? = null
    ) = sendDocument(chatId, path.toFile(), caption, replyToMessageId)
}

val CommandContext.chatId: Long
    get() = update.message?.chat?.id
        ?: update.callbackQuery?.message?.chat?.id
        ?: error("No chatId in update")

val CommandContext.replyToMessageId: Long?
    get() = update.message?.messageId
        ?: update.callbackQuery?.message?.messageId
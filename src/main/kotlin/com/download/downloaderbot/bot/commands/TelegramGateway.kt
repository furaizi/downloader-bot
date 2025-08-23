package com.download.downloaderbot.bot.commands

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.TelegramFile
import com.github.kotlintelegrambot.entities.inputmedia.GroupableMedia
import com.github.kotlintelegrambot.entities.inputmedia.InputMediaPhoto
import com.github.kotlintelegrambot.entities.inputmedia.MediaGroup
import org.springframework.beans.factory.ObjectProvider
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component
import java.io.File
import java.nio.file.Path

@Component
class TelegramGateway(private val botProvider: ObjectProvider<Bot>) {

    private val bot: Bot by lazy { botProvider.getObject() }

    suspend fun replyText(chatId: Long, text: String) {
        bot.sendMessage(ChatId.fromId(chatId), text)
    }

    suspend fun sendVideo(
        chatId: Long,
        file: File,
        caption: String? = null,
        durationSeconds: Int? = null,
        width: Int? = null,
        height: Int? = null
    ) {
        bot.sendVideo(
            chatId = ChatId.fromId(chatId),
            video = TelegramFile.ByFile(file),
            caption = caption,
            duration = durationSeconds,
            width = width,
            height = height
        )
    }

    suspend fun sendVideo(
        chatId: Long,
        path: Path,
        caption: String? = null,
        durationSeconds: Int? = null,
        width: Int? = null,
        height: Int? = null
    ) = sendVideo(chatId, path.toFile(),
            caption, durationSeconds, width, height)

    suspend fun sendPhoto(
        chatId: Long,
        file: File,
        caption: String? = null
    ) {
        bot.sendPhoto(
            chatId = ChatId.fromId(chatId),
            photo = TelegramFile.ByFile(file),
            caption = caption
        )
    }

    suspend fun sendPhoto(
        chatId: Long,
        path: Path,
        caption: String? = null
    ) = sendPhoto(chatId, path.toFile(), caption)

    suspend fun sendPhotosAlbum(
        chatId: Long,
        files: List<File>,
        caption: String? = null
    ) {
        val media = files.mapIndexed { index, file ->
            InputMediaPhoto(
                media = TelegramFile.ByFile(file),
                caption = caption.takeIf { index == 0 }
            )
        }.toTypedArray()

        bot.sendMediaGroup(
            chatId = ChatId.fromId(chatId),
            mediaGroup = MediaGroup.from(*media)
        )
    }

    suspend fun sendPhotosAlbumChunked(
        chatId: Long,
        files: List<File>,
        caption: String? = null
    ) {
        files.chunked(10).forEachIndexed { chunkIndex, chunk ->
            val shouldCaption = chunkIndex == 0
            sendPhotosAlbum(
                chatId = chatId,
                files = chunk,
                caption = caption.takeIf { shouldCaption })
        }
    }

    // Compiler doesn't allow to have both List<File> and List<Path> overloads
    // due to type erasure
    suspend fun sendPhotosAlbumFromPaths(
        chatId: Long,
        paths: List<Path>,
        caption: String? = null
    ) = sendPhotosAlbum(chatId, paths.map { it.toFile() }, caption)

    suspend fun sendPhotosAlbumChunkedFromPaths(
        chatId: Long,
        paths: List<Path>,
        caption: String? = null
    ) = sendPhotosAlbumChunked(chatId, paths.map { it.toFile() }, caption)

    suspend fun sendAudio(
        chatId: Long,
        file: File,
        durationSeconds: Int? = null,
        performer: String? = null,
        title: String? = null
    ) {
        bot.sendAudio(
            chatId = ChatId.fromId(chatId),
            audio = TelegramFile.ByFile(file),
            duration = durationSeconds,
            performer = performer,
            title = title,
        )
    }

    suspend fun sendAudio(
        chatId: Long,
        path: Path,
        durationSeconds: Int? = null,
        performer: String? = null,
        title: String? = null,
    ) = sendAudio(chatId, path.toFile(),
        durationSeconds, performer, title)

    suspend fun sendDocument(
        chatId: Long,
        file: File,
        caption: String? = null
    ) {
        bot.sendDocument(
            chatId = ChatId.fromId(chatId),
            document = TelegramFile.ByFile(file),
            caption = caption
        )
    }

    suspend fun sendDocument(
        chatId: Long,
        path: Path,
        caption: String? = null
    ) = sendDocument(chatId, path.toFile(), caption)
}

val CommandContext.chatId: Long
    get() = update.message?.chat?.id
        ?: update.callbackQuery?.message?.chat?.id
        ?: error("No chatId in update")
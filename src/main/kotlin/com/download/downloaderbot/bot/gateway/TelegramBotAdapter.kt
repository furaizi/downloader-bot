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

@Component
class TelegramBotAdapter(
    private val botProvider: ObjectProvider<Bot>
) : BotPort {

    private val bot: Bot by lazy { botProvider.getObject() }

    override suspend fun sendText(
        chatId: Long,
        text: String,
        replyToMessageId: Long?
    ) {
        bot.sendMessage(
            chatId = ChatId.fromId(chatId),
            text = text,
            replyToMessageId = replyToMessageId
        )
    }

    override suspend fun sendVideo(
        chatId: Long,
        file: File,
        caption: String?,
        durationSeconds: Int?,
        width: Int?,
        height: Int?,
        replyToMessageId: Long?
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

    override suspend fun sendPhoto(
        chatId: Long,
        file: File,
        caption: String?,
        replyToMessageId: Long?
    ) {
        bot.sendPhoto(
            chatId = ChatId.fromId(chatId),
            photo = TelegramFile.ByFile(file),
            caption = caption,
            replyToMessageId = replyToMessageId
        )
    }

    override suspend fun sendPhotoAlbum(
        chatId: Long,
        files: List<File>,
        caption: String?,
        replyToMessageId: Long?
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

    override suspend fun sendAudio(
        chatId: Long,
        file: File,
        durationSeconds: Int?,
        performer: String?,
        title: String?,
        replyToMessageId: Long?
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

    override suspend fun sendDocument(
        chatId: Long,
        file: File,
        caption: String?,
        replyToMessageId: Long?
    ) {
        bot.sendDocument(
            chatId = ChatId.fromId(chatId),
            document = TelegramFile.ByFile(file),
            caption = caption,
            replyToMessageId = replyToMessageId
        )
    }

}

val CommandContext.chatId: Long
    get() = update.message?.chat?.id
        ?: update.callbackQuery?.message?.chat?.id
        ?: error("No chatId in update")

val CommandContext.replyToMessageId: Long?
    get() = update.message?.messageId
        ?: update.callbackQuery?.message?.messageId

val CommandContext.chatType: String
    get() = update.message?.chat?.type
        ?: update.callbackQuery?.message?.chat?.type
        ?: error("No chat type in update")

val CommandContext.isPrivateChat: Boolean
    get() = chatType == "private"

val CommandContext.isGroupChat: Boolean
    get() = chatType == "group" || chatType == "supergroup"

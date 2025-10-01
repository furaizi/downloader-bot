package com.download.downloaderbot.bot.gateway.telegram

import com.download.downloaderbot.bot.commands.CommandContext
import com.download.downloaderbot.bot.gateway.BotPort
import com.download.downloaderbot.bot.gateway.GatewayResult
import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.Message
import com.github.kotlintelegrambot.entities.TelegramFile
import com.github.kotlintelegrambot.entities.files.PhotoSize
import com.github.kotlintelegrambot.entities.inputmedia.InputMediaPhoto
import com.github.kotlintelegrambot.entities.inputmedia.MediaGroup
import org.springframework.beans.factory.ObjectProvider
import org.springframework.stereotype.Component
import java.io.File
import kotlin.math.max

@Component
class TelegramBotAdapter(
    private val botProvider: ObjectProvider<Bot>
) : BotPort {

    private val bot: Bot by lazy { botProvider.getObject() }

    override suspend fun sendText(
        chatId: Long,
        text: String,
        replyToMessageId: Long?
    ): GatewayResult<Message> =
        bot.sendMessage(
            chatId = ChatId.fromId(chatId),
            text = text,
            replyToMessageId = replyToMessageId
        ).toGateway()

    override suspend fun sendVideo(
        chatId: Long,
        file: File,
        caption: String?,
        durationSeconds: Int?,
        width: Int?,
        height: Int?,
        replyToMessageId: Long?
    ): GatewayResult<Message> =
        bot.sendVideo(
            chatId = ChatId.fromId(chatId),
            video = TelegramFile.ByFile(file),
            caption = caption,
            duration = durationSeconds,
            width = width,
            height = height,
            replyToMessageId = replyToMessageId
        ).toGateway()

    override suspend fun sendVideo(
        chatId: Long,
        fileId: String,
        caption: String?,
        durationSeconds: Int?,
        width: Int?,
        height: Int?,
        replyToMessageId: Long?
    ): GatewayResult<Message> =
        bot.sendVideo(
            chatId = ChatId.fromId(chatId),
            video = TelegramFile.ByFileId(fileId),
            caption = caption,
            duration = durationSeconds,
            width = width,
            height = height,
            replyToMessageId = replyToMessageId
        ).toGateway()

    override suspend fun sendPhoto(
        chatId: Long,
        file: File,
        caption: String?,
        replyToMessageId: Long?
    ): GatewayResult<Message> =
        bot.sendPhoto(
            chatId = ChatId.fromId(chatId),
            photo = TelegramFile.ByFile(file),
            caption = caption,
            replyToMessageId = replyToMessageId
        ).toGateway()

    override suspend fun sendPhoto(
        chatId: Long,
        fileId: String,
        caption: String?,
        replyToMessageId: Long?
    ): GatewayResult<Message> =
        bot.sendPhoto(
            chatId = ChatId.fromId(chatId),
            photo = TelegramFile.ByFileId(fileId),
            caption = caption,
            replyToMessageId = replyToMessageId
        ).toGateway(
    )

    override suspend fun sendPhotoAlbum(
        chatId: Long,
        files: List<File>,
        caption: String?,
        replyToMessageId: Long?
    ): GatewayResult<List<Message>> {
        val media = files.mapIndexed { index, file ->
            InputMediaPhoto(
                media = TelegramFile.ByFile(file),
                caption = caption.takeIf { index == 0 }
            )
        }.toTypedArray()

        return bot.sendMediaGroup(
            chatId = ChatId.fromId(chatId),
            mediaGroup = MediaGroup.from(*media),
            replyToMessageId = replyToMessageId
        ).toGateway()
    }

    override suspend fun sendPhotoAlbum(
        chatId: Long,
        fileIds: List<String>,
        caption: String?,
        replyToMessageId: Long?
    ): GatewayResult<List<Message>> {
        val media = fileIds.mapIndexed { index, fileId ->
            InputMediaPhoto(
                media = TelegramFile.ByFileId(fileId),
                caption = caption.takeIf { index == 0 }
            )
        }.toTypedArray()

        return bot.sendMediaGroup(
            chatId = ChatId.fromId(chatId),
            mediaGroup = MediaGroup.from(*media),
            replyToMessageId = replyToMessageId
        ).toGateway()
    }

    override suspend fun sendAudio(
        chatId: Long,
        file: File,
        durationSeconds: Int?,
        performer: String?,
        title: String?,
        replyToMessageId: Long?
    ): GatewayResult<Message> =
        bot.sendAudio(
            chatId = ChatId.fromId(chatId),
            audio = TelegramFile.ByFile(file),
            duration = durationSeconds,
            performer = performer,
            title = title,
            replyToMessageId = replyToMessageId
        ).toGateway()

    override suspend fun sendAudio(
        chatId: Long,
        fileId: String,
        durationSeconds: Int?,
        performer: String?,
        title: String?,
        replyToMessageId: Long?
    ): GatewayResult<Message> =
        bot.sendAudio(
            chatId = ChatId.fromId(chatId),
            audio = TelegramFile.ByFileId(fileId),
            duration = durationSeconds,
            performer = performer,
            title = title,
            replyToMessageId = replyToMessageId
        ).toGateway()

    override suspend fun sendDocument(
        chatId: Long,
        file: File,
        caption: String?,
        replyToMessageId: Long?
    ): GatewayResult<Message> =
        bot.sendDocument(
            chatId = ChatId.fromId(chatId),
            document = TelegramFile.ByFile(file),
            caption = caption,
            replyToMessageId = replyToMessageId
        ).toGateway()

    override suspend fun sendDocument(
        chatId: Long,
        fileId: String,
        caption: String?,
        replyToMessageId: Long?
    ): GatewayResult<Message> =
        bot.sendDocument(
            chatId = ChatId.fromId(chatId),
            document = TelegramFile.ByFileId(fileId),
            caption = caption,
            replyToMessageId = replyToMessageId
        ).toGateway()

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

val Message.fileId: String?
    get() = this.photo.largest()?.fileId
    ?: this.document?.fileId
    ?: this.video?.fileId
    ?: this.audio?.fileId
    ?: this.animation?.fileId

val Message.fileUniqueId: String?
    get() = this.photo.largest()?.fileUniqueId
    ?: this.document?.fileUniqueId
    ?: this.video?.fileUniqueId
    ?: this.audio?.fileUniqueId
    ?: this.animation?.fileUniqueId

private fun List<PhotoSize>?.largest(): PhotoSize? =
    this?.maxByOrNull { max(it.width, it.height) }

package com.download.downloaderbot.bot.gateway.telegram

import com.download.downloaderbot.bot.commands.CommandContext
import com.download.downloaderbot.bot.gateway.AudioOptions
import com.download.downloaderbot.bot.gateway.BotPort
import com.download.downloaderbot.bot.gateway.GatewayResult
import com.download.downloaderbot.bot.gateway.InputFile
import com.download.downloaderbot.bot.gateway.MediaInput
import com.download.downloaderbot.bot.gateway.MessageOptions
import com.download.downloaderbot.bot.gateway.VideoOptions
import com.download.downloaderbot.core.domain.MediaType
import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.Message
import com.github.kotlintelegrambot.entities.ReplyMarkup
import com.github.kotlintelegrambot.entities.files.PhotoSize
import com.github.kotlintelegrambot.entities.inputmedia.GroupableMedia
import com.github.kotlintelegrambot.entities.inputmedia.InputMediaPhoto
import com.github.kotlintelegrambot.entities.inputmedia.InputMediaVideo
import com.github.kotlintelegrambot.entities.inputmedia.MediaGroup
import org.springframework.beans.factory.ObjectProvider
import org.springframework.stereotype.Component
import kotlin.math.max

@Component
class TelegramBotAdapter(
    private val botProvider: ObjectProvider<Bot>,
) : BotPort {
    private val bot: Bot by lazy { botProvider.getObject() }

    override suspend fun sendText(
        chatId: Long,
        text: String,
        replyToMessageId: Long?,
        replyMarkup: ReplyMarkup?,
    ): GatewayResult<Message> =
        bot.sendMessage(
            chatId = ChatId.fromId(chatId),
            text = text,
            replyToMessageId = replyToMessageId,
            replyMarkup = replyMarkup,
        ).toGateway()

    override suspend fun sendPhoto(
        chatId: Long,
        file: InputFile,
        options: MessageOptions,
    ): GatewayResult<Message> =
        bot.sendPhoto(
            chatId = ChatId.fromId(chatId),
            photo = file.toTelegram(),
            caption = options.caption,
            replyToMessageId = options.replyToMessageId,
            replyMarkup = options.replyMarkup,
        ).toGateway()

    override suspend fun sendVideo(
        chatId: Long,
        file: InputFile,
        messageOptions: MessageOptions,
        videoOptions: VideoOptions,
    ): GatewayResult<Message> =
        bot.sendVideo(
            chatId = ChatId.fromId(chatId),
            video = file.toTelegram(),
            caption = messageOptions.caption,
            duration = videoOptions.durationSeconds,
            width = videoOptions.width,
            height = videoOptions.height,
            replyToMessageId = messageOptions.replyToMessageId,
            replyMarkup = messageOptions.replyMarkup,
        ).toGateway()

    override suspend fun sendAudio(
        chatId: Long,
        file: InputFile,
        messageOptions: MessageOptions,
        audioOptions: AudioOptions,
    ): GatewayResult<Message> =
        bot.sendAudio(
            chatId = ChatId.fromId(chatId),
            audio = file.toTelegram(),
            duration = audioOptions.durationSeconds,
            performer = audioOptions.performer,
            title = audioOptions.title,
            replyToMessageId = messageOptions.replyToMessageId,
            replyMarkup = messageOptions.replyMarkup,
        ).toGateway()

    override suspend fun sendDocument(
        chatId: Long,
        file: InputFile,
        options: MessageOptions,
    ): GatewayResult<Message> =
        bot.sendDocument(
            chatId = ChatId.fromId(chatId),
            document = file.toTelegram(),
            caption = options.caption,
            replyToMessageId = options.replyToMessageId,
            replyMarkup = options.replyMarkup,
        ).toGateway()

    @Suppress("SpreadOperator")
    override suspend fun sendMediaAlbum(
        chatId: Long,
        items: List<MediaInput>,
        caption: String?,
        replyToMessageId: Long?,
    ): GatewayResult<List<Message>> {
        val media =
            items.mapIndexed { index, item ->
                val cap = caption.takeIf { index == 0 }
                val m: GroupableMedia = when (item.type) {
                    MediaType.IMAGE -> InputMediaPhoto(media = item.file.toTelegram(), caption = cap)
                    MediaType.VIDEO -> InputMediaVideo(media = item.file.toTelegram(), caption = cap)
                    MediaType.AUDIO -> error("Audio is currently not supported")
                }
                m
            }.toTypedArray()

        return bot.sendMediaGroup(
            chatId = ChatId.fromId(chatId),
            mediaGroup = MediaGroup.from(*media),
            replyToMessageId = replyToMessageId,
        ).toGateway()
    }
}

val CommandContext.chatId: Long
    get() =
        update.message?.chat?.id
            ?: update.callbackQuery?.message?.chat?.id
            ?: error("No chatId in update")

val CommandContext.replyToMessageId: Long?
    get() =
        update.message?.messageId
            ?: update.callbackQuery?.message?.messageId

val CommandContext.chatType: String
    get() =
        update.message?.chat?.type
            ?: update.callbackQuery?.message?.chat?.type
            ?: error("No chat type in update")

val CommandContext.isPrivateChat: Boolean
    get() = chatType == "private"

val CommandContext.isGroupChat: Boolean
    get() = chatType == "group" || chatType == "supergroup"

val Message.fileId: String?
    get() =
        this.photo.largest()?.fileId
            ?: this.document?.fileId
            ?: this.video?.fileId
            ?: this.audio?.fileId
            ?: this.animation?.fileId

val Message.fileUniqueId: String?
    get() =
        this.photo.largest()?.fileUniqueId
            ?: this.document?.fileUniqueId
            ?: this.video?.fileUniqueId
            ?: this.audio?.fileUniqueId
            ?: this.animation?.fileUniqueId

private fun List<PhotoSize>?.largest(): PhotoSize? = this?.maxByOrNull { max(it.width, it.height) }

package com.download.downloaderbot.bot.commands

import com.download.downloaderbot.bot.gateway.TelegramGateway
import com.download.downloaderbot.bot.gateway.chatId
import com.download.downloaderbot.bot.gateway.replyToMessageId
import com.download.downloaderbot.core.domain.MediaType
import com.download.downloaderbot.core.service.MediaDownloadService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import org.springframework.stereotype.Component
import java.io.File

private val log = KotlinLogging.logger {}

@Component
class DownloadCommand(
    private val mediaDownloadService: MediaDownloadService,
    private val gateway: TelegramGateway
) : BotCommand {
    override val name = "download"

    override suspend fun handle(ctx: CommandContext) {
        val replyTo = ctx.replyToMessageId
        val url = ctx.args.firstOrNull() ?: run {
            gateway.replyText(ctx.chatId, "Будь ласка, вкажіть URL для завантаження.", replyTo)
            return
        }

        val mediaList = withContext(Dispatchers.IO) {
            mediaDownloadService.download(url)
        }
        val firstMedia = mediaList.first()

        if (firstMedia.type == MediaType.IMAGE && mediaList.size in 2..10) {
            val files = mediaList.map { File(it.fileUrl) }
            gateway.sendPhotosAlbum(ctx.chatId, files, replyToMessageId = replyTo)
        }
        else if (firstMedia.type == MediaType.IMAGE && mediaList.size > 10) {
            val files = mediaList.map { File(it.fileUrl) }
            gateway.sendPhotosAlbumChunked(ctx.chatId, files, replyToMessageId = replyTo)
        }
        else {
            mediaList.forEach {
                val file = File(it.fileUrl)
                when (it.type) {
                    MediaType.VIDEO -> gateway.sendVideo(ctx.chatId, file, replyToMessageId = replyTo)
                    MediaType.AUDIO -> gateway.sendAudio(ctx.chatId, file, replyToMessageId = replyTo)
                    MediaType.IMAGE -> gateway.sendPhoto(ctx.chatId, file, replyToMessageId = replyTo)
                }
            }
        }

        log.info { "Downloaded: ${mediaList.map { it.title }} (${mediaList.map { it.fileUrl }})" }
    }
}
package com.download.downloaderbot.bot.commands

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
    gateway: TelegramGateway
) : CommandHandler(gateway) {
    override val name = "download"

    override suspend fun handle(ctx: CommandContext) {
        val url = ctx.args.first()

        val mediaList = withContext(Dispatchers.IO) {
            mediaDownloadService.download(url)
        }
        val firstMedia = mediaList.first()

        if (firstMedia.type == MediaType.IMAGE && mediaList.size in 2..10) {
            val files = mediaList.map { File(it.fileUrl) }
            gateway.sendPhotosAlbum(ctx.chatId, files)
        }
        else if (firstMedia.type == MediaType.IMAGE && mediaList.size > 10) {
            val files = mediaList.map { File(it.fileUrl) }
            gateway.sendPhotosAlbumChunked(ctx.chatId, files)
        }
        else {
            mediaList.forEach {
                val file = File(it.fileUrl)
                when (it.type) {
                    MediaType.VIDEO -> gateway.sendVideo(ctx.chatId, file)
                    MediaType.AUDIO -> gateway.sendAudio(ctx.chatId, file)
                    MediaType.IMAGE -> gateway.sendPhoto(ctx.chatId, file)
                }
            }
        }

        log.info { "Downloaded: ${mediaList.map { it.title }} (${mediaList.map { it.fileUrl }})" }
    }
}
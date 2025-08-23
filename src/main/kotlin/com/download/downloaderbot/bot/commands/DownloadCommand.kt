package com.download.downloaderbot.bot.commands

import com.download.downloaderbot.core.domain.MediaType
import com.download.downloaderbot.core.usecase.MediaDownloadService
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

        val media = withContext(Dispatchers.IO) {
            mediaDownloadService.download(url)
        }
        val file = File(media.fileUrl)

        when (media.type) {
            MediaType.VIDEO -> gateway.sendVideo(ctx.chatId, file)
            MediaType.AUDIO -> gateway.sendAudio(ctx.chatId, file)
            MediaType.IMAGE -> gateway.sendPhoto(ctx.chatId, file)
        }

        log.info { "Downloaded: ${media.title} (${file.name})" }

    }
}
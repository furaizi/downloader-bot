package com.download.downloaderbot.bot.commands

import com.download.downloaderbot.core.entity.MediaType
import com.download.downloaderbot.core.service.MediaService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import org.springframework.stereotype.Component
import java.io.File

private val log = KotlinLogging.logger {}

@Component
class DownloadCommand(
    private val mediaService: MediaService
) : CommandHandler {
    override val name = "download"

    override suspend fun handle(ctx: CommandContext) {
        val gateway = ctx.gateway
        val url = ctx.args.first()

        val media = withContext(Dispatchers.IO) {
            mediaService.download(url)
        }
        val file = File(media.path)

        when (media.type) {
            MediaType.VIDEO -> gateway.sendVideo(ctx.chatId, file,
                durationSeconds = media.duration.toInt())
            MediaType.AUDIO -> gateway.sendAudio(ctx.chatId, file,
                durationSeconds = media.duration.toInt())
            MediaType.IMAGE -> gateway.sendPhoto(ctx.chatId, file)
        }

        log.info { "Downloaded: ${media.title} (${file.name})" }

    }
}
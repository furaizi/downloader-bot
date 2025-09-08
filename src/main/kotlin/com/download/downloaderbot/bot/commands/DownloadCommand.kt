package com.download.downloaderbot.bot.commands

import com.download.downloaderbot.core.domain.MediaType
import com.download.downloaderbot.core.queue.DownloadQueueService
import com.download.downloaderbot.core.service.MediaDownloadService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import org.springframework.stereotype.Component
import java.io.File

private val log = KotlinLogging.logger {}

@Component
class DownloadCommand(
    private val queue: DownloadQueueService,
    gateway: TelegramGateway
) : CommandHandler(gateway) {
    override val name = "download"

    override suspend fun handle(ctx: CommandContext) {
        val url = ctx.args.firstOrNull()
        if (url.isNullOrBlank()) {
            gateway.replyText(ctx.chatId, "Usage: /download <url>")
            return
        }

        val res = queue.enqueue(ctx.chatId, url)
        gateway.replyMarkdown(
            ctx.chatId,
            "Queued ✅\njobId: `${res.jobId}`\nCheck: `/status ${res.jobId}`"
        )
    }
}
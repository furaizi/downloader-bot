package com.download.downloaderbot.bot.commands

import com.download.downloaderbot.app.download.MediaService
import com.download.downloaderbot.bot.commands.util.UrlValidator
import com.download.downloaderbot.bot.gateway.BotPort
import com.download.downloaderbot.bot.gateway.telegram.chatId
import com.download.downloaderbot.bot.gateway.telegram.isGroupChat
import com.download.downloaderbot.bot.gateway.telegram.isPrivateChat
import com.download.downloaderbot.bot.gateway.telegram.replyToMessageId
import com.download.downloaderbot.bot.job.DownloadJob
import com.download.downloaderbot.bot.job.DownloadJobQueue
import com.download.downloaderbot.bot.ratelimit.guard.RateLimitGuard
import mu.KotlinLogging
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

@Component
class DownloadCommand(
    private val service: MediaService,
    private val botPort: BotPort,
    private val rateLimitGuard: RateLimitGuard,
    private val validator: UrlValidator,
    private val downloadJobQueue: DownloadJobQueue,
) : BotCommand {
    override val name: String = "download"

    override suspend fun handle(ctx: CommandContext) {
        val replyTo = ctx.replyToMessageId
        val url = ctx.args.firstOrNull()?.trim() ?: ""
        val isNotUrl = !validator.isHttpUrl(url)

        val allowed =
            when {
                isNotUrl -> false
                ctx.isPrivateChat -> true
                ctx.isGroupChat -> service.supports(url)
                else -> false
            }

        if (!allowed) {
            if (ctx.isPrivateChat) {
                log.info { "Executing /$name command but not URL provided" }
                rateLimitGuard.runOrReject(ctx) {
                    botPort.sendText(ctx.chatId, "Будь ласка, вкажіть URL для завантаження.", replyTo)
                }
            }
            return
        }

        log.info { "Scheduling /$name command with url: $url" }

        rateLimitGuard.runOrReject(ctx) {
            val job =
                DownloadJob(
                    sourceUrl = url,
                    chatId = ctx.chatId,
                    replyToMessageId = replyTo,
                    commandContext = ctx,
                )
            downloadJobQueue.submit(job)
        }
    }
}

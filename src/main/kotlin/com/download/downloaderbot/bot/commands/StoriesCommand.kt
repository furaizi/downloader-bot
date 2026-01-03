package com.download.downloaderbot.bot.commands

import com.download.downloaderbot.app.download.MediaService
import com.download.downloaderbot.bot.commands.util.InputValidator
import com.download.downloaderbot.bot.gateway.BotPort
import com.download.downloaderbot.bot.gateway.telegram.replyToMessageId
import com.download.downloaderbot.bot.job.DownloadJobQueue
import com.download.downloaderbot.bot.ratelimit.guard.RateLimitGuard
import mu.KotlinLogging
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

@Component
class StoriesCommand(
    private val service: MediaService,
    private val botPort: BotPort,
    private val rateLimitGuard: RateLimitGuard,
    private val validator: InputValidator,
    private val downloadJobQueue: DownloadJobQueue,
) : BotCommand {

    override val name = "stories"

    override suspend fun handle(ctx: CommandContext) {
        val replyTo = ctx.replyToMessageId
        val username = ctx.args.firstOrNull()?.trim() ?: ""
    }
}
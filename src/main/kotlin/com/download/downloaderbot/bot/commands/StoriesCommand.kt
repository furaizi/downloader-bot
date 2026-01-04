package com.download.downloaderbot.bot.commands

import com.download.downloaderbot.bot.commands.util.InputValidator
import com.download.downloaderbot.bot.commands.util.InstagramUrls
import com.download.downloaderbot.bot.gateway.BotPort
import com.download.downloaderbot.bot.gateway.telegram.chatId
import com.download.downloaderbot.bot.gateway.telegram.replyToMessageId
import com.download.downloaderbot.bot.job.DownloadJob
import com.download.downloaderbot.bot.job.DownloadJobQueue
import com.download.downloaderbot.bot.ratelimit.guard.RateLimitGuard
import mu.KotlinLogging
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

@Component
class StoriesCommand(
    private val botPort: BotPort,
    private val rateLimitGuard: RateLimitGuard,
    private val validator: InputValidator,
    private val downloadJobQueue: DownloadJobQueue,
) : BotCommand {
    override val name = "stories"

    override suspend fun handle(ctx: CommandContext) {
        val replyTo = ctx.replyToMessageId
        val username = ctx.args.firstOrNull()?.trim() ?: ""
        val isNotUsername = !validator.isInstagramUsername(username)

        if (isNotUsername) {
            log.info { "Executing /$name command but not username provided" }
            rateLimitGuard.runOrReject(ctx) {
                botPort.sendText(ctx.chatId, "Будь ласка, вкажіть дійсний username для завантаження історій.", replyTo)
            }
            return
        }

        log.info { "Scheduling /$name command with username: $username" }
        val url = InstagramUrls.stories(username)

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

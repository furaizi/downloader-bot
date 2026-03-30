package com.download.downloaderbot.bot.commands

import com.download.downloaderbot.app.download.MediaService
import com.download.downloaderbot.bot.commands.util.isNotHttpUrl
import com.download.downloaderbot.bot.gateway.telegram.chatId
import com.download.downloaderbot.bot.gateway.telegram.isGroupChat
import com.download.downloaderbot.bot.gateway.telegram.isPrivateChat
import com.download.downloaderbot.bot.gateway.telegram.replyToMessageId
import com.download.downloaderbot.bot.job.DownloadJobDispatcher
import com.download.downloaderbot.bot.job.DownloadJob
import com.download.downloaderbot.bot.ratelimit.guard.RateLimitGuard
import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.ChatId
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

@Component
class DownloadCommand(
    private val service: MediaService,
    private val bot: Bot,
    private val rateLimitGuard: RateLimitGuard,
    private val downloadJobDispatcher: DownloadJobDispatcher,
) : BotCommand {
    override val name: String = "download"

    override suspend fun handle(ctx: CommandContext) {
        val replyTo = ctx.replyToMessageId
        val url = ctx.args.firstOrNull()?.trim() ?: ""

        val allowed =
            when {
                url.isNotHttpUrl() -> false
                ctx.isPrivateChat -> true
                ctx.isGroupChat -> service.supports(url)
                else -> false
            }

        if (!allowed) {
            if (ctx.isPrivateChat) {
                log.info { "Executing /$name command but not URL provided" }
                rateLimitGuard.runOrReject(ctx) {
                    bot.sendMessage(
                        chatId = ChatId.fromId(ctx.chatId),
                        text = "Будь ласка, вкажіть URL для завантаження.",
                        replyToMessageId = replyTo,
                    )
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
            downloadJobDispatcher.submit(job)
        }
    }
}

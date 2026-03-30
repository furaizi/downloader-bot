package com.download.downloaderbot.bot.commands

import com.download.downloaderbot.bot.commands.util.isNotInstagramUsername
import com.download.downloaderbot.bot.gateway.telegram.chatId
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
class StoriesCommand(
    private val bot: Bot,
    private val rateLimitGuard: RateLimitGuard,
    private val downloadJobDispatcher: DownloadJobDispatcher,
) : BotCommand {
    override val name = "stories"

    override suspend fun handle(ctx: CommandContext) {
        val replyTo = ctx.replyToMessageId
        val raw = ctx.args.firstOrNull().orEmpty()
        val username = raw.trim().removePrefix("@")

        if (username.isNotInstagramUsername()) {
            log.info { "Executing /$name command but invalid username provided: '$username'" }
            rateLimitGuard.runOrReject(ctx) {
                bot.sendMessage(
                    chatId = ChatId.fromId(ctx.chatId),
                    text = "Будь ласка, вкажіть дійсний username для завантаження історій.",
                    replyToMessageId = replyTo,
                )
            }
            return
        }

        log.info { "Scheduling /$name command with username: $username" }
        val url = "https://www.instagram.com/stories/$username/"

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

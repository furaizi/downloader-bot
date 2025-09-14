package com.download.downloaderbot.bot.ratelimit

import com.download.downloaderbot.bot.commands.CommandContext
import com.download.downloaderbot.bot.gateway.TelegramGateway
import com.download.downloaderbot.bot.gateway.chatId
import mu.KotlinLogging
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

@Component
@ConditionalOnProperty(
    prefix = "downloader.ratelimit",
    name = ["enabled"],
    havingValue = "true",
)
class DefaultRateLimitGuard(
    private val limiter: RateLimiter,
    private val gateway: TelegramGateway
) : RateLimitGuard {
    override suspend fun runOrReject(ctx: CommandContext, block: suspend () -> Unit) {
        val chatId = ctx.chatId
        val chatType = if (chatId < 0) "group" else "private"

        val ok = limiter.tryConsumePerChatOrGroup(ctx.chatId)
        if (!ok) {
            log.info { "[rate-limit] REJECT chatId=$chatId type=$chatType -> tell user" }
            gateway.replyText(ctx.chatId, "Too many requests. Please try again later.")
            return
        }
        log.debug { "[rate-limit] local PASSED chatId=$chatId type=$chatType -> await global" }

        limiter.awaitGlobal()
        log.debug { "[rate-limit] global PASSED chatId=$chatId -> proceed handler" }

        block()
    }
}
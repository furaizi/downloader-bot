package com.download.downloaderbot.bot.ratelimit.guard

import com.download.downloaderbot.bot.commands.CommandContext
import com.download.downloaderbot.bot.gateway.telegram.chatId
import com.download.downloaderbot.bot.ratelimit.limiter.RateLimiter
import com.download.downloaderbot.core.downloader.TooManyRequestsException
import io.github.oshai.kotlinlogging.KotlinLogging

private val log = KotlinLogging.logger {}

class RateLimitGuard(
    private val limiter: RateLimiter? = null,
) {
    suspend fun <T> runOrReject(
        ctx: CommandContext,
        block: suspend () -> T,
    ): T {
        val limiter = limiter ?: return block()

        val chatId = ctx.chatId
        val chatType = if (chatId < 0) "group" else "private"

        val ok = limiter.tryConsumePerChatOrGroup(ctx.chatId)
        if (!ok) {
            log.info { "[rate-limit] REJECT chatId=$chatId type=$chatType -> tell user" }
            throw TooManyRequestsException(chatType, chatId)
        }
        log.debug { "[rate-limit] local PASSED chatId=$chatId type=$chatType -> await global" }

        limiter.awaitGlobal()
        log.debug { "[rate-limit] global PASSED chatId=$chatId -> proceed handler" }

        return block()
    }
}

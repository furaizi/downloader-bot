package com.download.downloaderbot.bot.ratelimit

import com.download.downloaderbot.bot.commands.CommandContext
import com.download.downloaderbot.bot.gateway.TelegramGateway
import com.download.downloaderbot.bot.gateway.chatId
import org.springframework.stereotype.Component

@Component
class RateLimitGuard(
    private val limiter: RateLimiter,
    private val gateway: TelegramGateway
) {
    suspend fun runOrReject(ctx: CommandContext, block: suspend () -> Unit) {
        val ok = limiter.tryConsumePerChatOrGroup(ctx.chatId)
        if (!ok) {
            gateway.replyText(ctx.chatId, "Too many requests. Please try again later.")
            return
        }
        limiter.awaitGlobal()
        block()
    }
}
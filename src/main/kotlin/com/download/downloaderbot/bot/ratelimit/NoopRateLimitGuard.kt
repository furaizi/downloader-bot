package com.download.downloaderbot.bot.ratelimit

import com.download.downloaderbot.bot.commands.CommandContext
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component

@Component
@Primary
@ConditionalOnProperty(
    prefix = "downloader.ratelimit",
    name = ["enabled"],
    havingValue = "false",
    matchIfMissing = true,
)
class NoopRateLimitGuard : RateLimitGuard {
    override suspend fun runOrReject(ctx: CommandContext, block: suspend () -> Unit) = block()
}
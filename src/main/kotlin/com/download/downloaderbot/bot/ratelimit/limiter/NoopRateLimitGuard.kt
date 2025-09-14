package com.download.downloaderbot.bot.ratelimit.limiter

import com.download.downloaderbot.bot.commands.CommandContext

class NoopRateLimitGuard : RateLimitGuard {
    override suspend fun runOrReject(ctx: CommandContext, block: suspend () -> Unit) = block()
}
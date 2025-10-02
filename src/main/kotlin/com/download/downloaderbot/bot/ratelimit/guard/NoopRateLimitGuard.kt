package com.download.downloaderbot.bot.ratelimit.guard

import com.download.downloaderbot.bot.commands.CommandContext

class NoopRateLimitGuard : RateLimitGuard {
    override suspend fun <T> runOrReject(
        ctx: CommandContext,
        block: suspend () -> T,
    ): T = block()
}

package com.download.downloaderbot.bot.ratelimit.guard

import com.download.downloaderbot.bot.commands.CommandContext

interface RateLimitGuard {
    suspend fun <T> runOrReject(
        ctx: CommandContext,
        block: suspend () -> T,
    ): T
}

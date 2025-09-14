package com.download.downloaderbot.bot.ratelimit.limiter

import com.download.downloaderbot.bot.commands.CommandContext

interface RateLimitGuard {
    suspend fun runOrReject(ctx: CommandContext, block: suspend () -> Unit)
}
package com.download.downloaderbot.bot.ratelimit

import com.download.downloaderbot.bot.commands.CommandContext

interface RateLimitGuard {
    suspend fun runOrReject(ctx: CommandContext, block: suspend () -> Unit)
}
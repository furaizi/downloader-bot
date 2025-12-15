package com.download.downloaderbot.bot.ratelimit.guard

import com.download.downloaderbot.bot.commands.CommandContext
import com.download.downloaderbot.bot.gateway.telegram.chatId
import com.download.downloaderbot.core.downloader.TooManyRequestsException

class RejectAllRateLimitGuard : RateLimitGuard {
    override suspend fun <T> runOrReject(ctx: CommandContext, block: suspend () -> T): T {
        throw TooManyRequestsException("test", ctx.chatId)
    }
}
package com.download.downloaderbot.bot.ratelimit.guard

import com.download.downloaderbot.bot.ratelimit.limiter.RateLimiter

class RejectAllRateLimiter : RateLimiter {
    override suspend fun awaitGlobal() = Unit

    override suspend fun tryConsumePerChatOrGroup(chatId: Long): Boolean = false
}

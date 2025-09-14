package com.download.downloaderbot.bot.ratelimit.limiter

interface RateLimiter {
    suspend fun awaitGlobal()
    suspend fun tryConsumePerChatOrGroup(chatId: Long): Boolean
}
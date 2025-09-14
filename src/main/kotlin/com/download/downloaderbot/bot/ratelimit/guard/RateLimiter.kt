package com.download.downloaderbot.bot.ratelimit.guard

interface RateLimiter {
    suspend fun awaitGlobal()
    suspend fun tryConsumePerChatOrGroup(chatId: Long): Boolean
}
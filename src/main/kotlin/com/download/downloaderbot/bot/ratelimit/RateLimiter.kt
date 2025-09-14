package com.download.downloaderbot.bot.ratelimit

interface RateLimiter {
    suspend fun awaitGlobal()
    suspend fun tryConsumePerChatOrGroup(chatId: Long): Boolean
}
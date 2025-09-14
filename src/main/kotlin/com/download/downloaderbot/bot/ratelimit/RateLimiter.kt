package com.download.downloaderbot.bot.ratelimit

interface RateLimiter {
    suspend fun allow(chatId: Long): Boolean
}
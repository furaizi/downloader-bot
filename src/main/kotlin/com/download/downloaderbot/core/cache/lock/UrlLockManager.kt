package com.download.downloaderbot.core.cache.lock

import java.time.Duration

interface UrlLockManager {
    suspend fun tryAcquire(url: String, ttl: Duration): String?
    suspend fun release(url: String, token: String)
}
package com.download.downloaderbot.core.cache.media

import com.download.downloaderbot.core.domain.Media
import java.time.Duration

interface MediaCache {
    suspend fun get(sourceUrl: String): List<Media>?
    suspend fun put(media: List<Media>, ttl: Duration = Duration.ofDays(7))
    suspend fun evict(sourceUrl: String)
}
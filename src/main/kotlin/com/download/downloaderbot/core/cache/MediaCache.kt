package com.download.downloaderbot.core.cache

import com.download.downloaderbot.core.domain.Media
import java.time.Duration

interface MediaCache {
    suspend fun getBySourceUrl(sourceUrl: String): Media?
    suspend fun put(media: Media, ttl: Duration = Duration.ofDays(7)) // TODO: make ttl configurable
    suspend fun evict(sourceUrl: String)
}
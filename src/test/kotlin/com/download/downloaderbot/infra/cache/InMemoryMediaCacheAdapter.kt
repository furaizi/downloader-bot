package com.download.downloaderbot.infra.cache

import com.download.downloaderbot.core.cache.CachePort
import com.download.downloaderbot.core.domain.Media
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

class InMemoryMediaCacheAdapter : CachePort<String, List<Media>> {
    private val storage = ConcurrentHashMap<String, List<Media>>()
    var lastPutTtl: Duration? = null
        private set

    fun clear() = storage.clear()

    override suspend fun get(id: String): List<Media>? = storage[id]

    override suspend fun put(
        id: String,
        values: List<Media>,
        ttl: Duration,
    ) {
        storage[id] = values
        lastPutTtl = ttl
    }

    override suspend fun evict(id: String) {
        storage.remove(id)
    }
}
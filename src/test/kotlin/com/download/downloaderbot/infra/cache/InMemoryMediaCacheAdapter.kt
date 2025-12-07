package com.download.downloaderbot.infra.cache

import com.download.downloaderbot.core.cache.CachePort
import com.download.downloaderbot.core.domain.Media
import org.springframework.boot.test.context.TestComponent
import org.springframework.context.annotation.Primary
import java.time.Duration

@Primary
@TestComponent
class InMemoryMediaCacheAdapter : CachePort<String, List<Media>> {
    private val storage = mutableMapOf<String, List<Media>>()
    final var lastPutTtl: Duration? = null
        private set

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
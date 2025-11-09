package com.download.downloaderbot.core.cache

import java.time.Duration

interface CachePort<ID, V> {
    suspend fun get(id: ID): V?

    suspend fun put(
        id: ID,
        values: V,
        ttl: Duration,
    )

    suspend fun evict(id: ID)
}

package com.download.downloaderbot.infra.cache

import com.download.downloaderbot.core.cache.CachePort
import com.download.downloaderbot.core.cache.CachedMedia
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class AsyncRedisMediaCacheAdapter(
    private val mediaTemplate: ReactiveRedisTemplate<String, List<CachedMedia>>
) : CachePort<String, List<CachedMedia>> {

    private fun key(url: String) = "media:url:$url"

    override suspend fun get(id: String): List<CachedMedia>? =
        mediaTemplate.opsForValue()
            .get(key(url = id))
            .awaitFirstOrNull()

    override suspend fun put(id: String, values: List<CachedMedia>, ttl: Duration) {
        if (values.isEmpty()) return
        mediaTemplate.opsForValue()
            .set(key(url = id), values, ttl)
            .awaitFirstOrNull()
    }

    override suspend fun evict(id: String) {
        mediaTemplate.delete(key(url = id))
            .awaitFirstOrNull()
    }
}
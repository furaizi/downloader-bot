package com.download.downloaderbot.infra.cache

import com.download.downloaderbot.core.cache.CachePort
import com.download.downloaderbot.core.domain.Media
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.data.redis.core.ReactiveRedisTemplate
import java.time.Duration

class AsyncRedisMediaCacheAdapter(
    private val mediaTemplate: ReactiveRedisTemplate<String, List<Media>>
) : CachePort<String, List<Media>> {

    private fun key(url: String) = "media:url:$url"

    override suspend fun get(id: String): List<Media>? =
        mediaTemplate.opsForValue()
            .get(key(url = id))
            .awaitFirstOrNull()

    override suspend fun put(id: String, values: List<Media>, ttl: Duration) {
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
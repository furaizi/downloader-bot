package com.download.downloaderbot.core.cache

import com.download.downloaderbot.core.domain.Media
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class AsyncRedisMediaCache(
    private val mediaTemplate: ReactiveRedisTemplate<String, List<Media>>
) : MediaCache {

    private fun key(url: String) = "media:url:$url"

    override suspend fun get(sourceUrl: String): List<Media>? =
        mediaTemplate.opsForValue()
            .get(key(sourceUrl))
            .awaitFirstOrNull()

    override suspend fun put(media: List<Media>, ttl: Duration) {
        val url = media.first().sourceUrl
        mediaTemplate.opsForValue()
            .set(key(url), media, ttl)
            .awaitFirstOrNull()
    }

    override suspend fun evict(sourceUrl: String) {
        mediaTemplate.delete(key(sourceUrl))
            .awaitFirstOrNull()
    }

}
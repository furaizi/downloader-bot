package com.download.downloaderbot.core.cache

import com.download.downloaderbot.core.domain.Media
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.data.redis.core.ReactiveRedisTemplate
import java.security.MessageDigest
import java.time.Duration

class RedisMediaCache(
    private val mediaTemplate: ReactiveRedisTemplate<String, Media>
) : MediaCache {

    private fun key(url: String) = "media:bySource:${hash(url)}"

    override suspend fun getBySourceUrl(sourceUrl: String): Media? =
        mediaTemplate.opsForValue()
            .get(key(sourceUrl))
            .awaitFirstOrNull()

    override suspend fun put(media: Media, ttl: Duration) {
        mediaTemplate.opsForValue()
            .set(key(media.sourceUrl), media, ttl)
            .awaitFirstOrNull()
    }

    override suspend fun evict(sourceUrl: String) {
        mediaTemplate.delete(key(sourceUrl))
            .awaitFirstOrNull()
    }

    private fun hash(s: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(s.toByteArray())
            .joinToString("") {
                "%02x".format(it)
            }
            .take(16)
}
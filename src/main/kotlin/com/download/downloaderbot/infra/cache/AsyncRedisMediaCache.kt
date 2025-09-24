package com.download.downloaderbot.infra.cache

import com.download.downloaderbot.core.cache.MediaCache
import com.download.downloaderbot.core.domain.Media
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.core.ReactiveStringRedisTemplate
import org.springframework.stereotype.Component
import java.nio.file.Path
import java.time.Duration

@Component
class AsyncRedisMediaCache(
    private val mediaTemplate: ReactiveRedisTemplate<String, List<Media>>,
    private val stringTemplate: ReactiveStringRedisTemplate
) : MediaCache {

    private fun key(url: String) = "media:url:$url"
    private fun fileIndexKey(fileUrl: String) = "media:file:$fileUrl"

    override suspend fun get(sourceUrl: String): List<Media>? =
        mediaTemplate.opsForValue()
            .get(key(sourceUrl))
            .awaitFirstOrNull()

    override suspend fun put(media: List<Media>, ttl: Duration) {
        if (media.isEmpty()) return
        val sourceUrl = media.first().sourceUrl

        mediaTemplate.opsForValue()
            .set(key(sourceUrl), media, ttl)
            .awaitFirstOrNull()

        media.asSequence()
            .map { it.fileUrl }
            .toSet()
            .forEach { fileUrl ->
                val idxKey = fileIndexKey(fileUrl)
                stringTemplate.opsForSet()
                    .add(idxKey, sourceUrl)
                    .awaitFirstOrNull()
                stringTemplate.expire(idxKey, ttl)
                    .awaitFirstOrNull()
            }
    }

    override suspend fun evict(sourceUrl: String) {
        val cachedMedia = get(sourceUrl).orEmpty()
        val setOps = stringTemplate.opsForSet()

        mediaTemplate.delete(key(sourceUrl))
            .awaitFirstOrNull()

        cachedMedia.asSequence()
            .map { it.fileUrl }
            .toSet()
            .forEach { fileUrl ->
                val idxKey = fileIndexKey(fileUrl)
                setOps.remove(idxKey, sourceUrl).awaitFirstOrNull()
                val size = setOps.size(idxKey).awaitFirstOrNull() ?: 0
                if (size == 0L) {
                    stringTemplate.delete(idxKey).awaitFirstOrNull()
                }
            }
    }

    override suspend fun evictByPath(path: Path) {
        val idxKey = fileIndexKey(path.toAbsolutePath().toString())

        val sources = stringTemplate.opsForSet()
            .members(idxKey)
            .collectList()
            .awaitFirstOrNull()
            .orEmpty()

        sources.forEach { sourceUrl ->
            mediaTemplate.delete(key(sourceUrl)).awaitFirstOrNull()
        }

        stringTemplate.delete(idxKey).awaitFirstOrNull()
    }
}

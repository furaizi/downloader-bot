package com.download.downloaderbot.infra.cache

import com.download.downloaderbot.core.cache.CachePort
import com.download.downloaderbot.core.domain.Media
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.core.ReactiveStringRedisTemplate
import org.springframework.stereotype.Component
import java.nio.file.Path
import java.time.Duration

@Component
class AsyncRedisMediaCacheAdapter(
    private val mediaTemplate: ReactiveRedisTemplate<String, List<Media>>,
    private val stringTemplate: ReactiveStringRedisTemplate
) : CachePort<String, List<Media>> {

    private fun key(url: String) = "media:url:$url"
    private fun fileIndexKey(fileUrl: String) = "media:file:$fileUrl"

    override suspend fun get(id: String): List<Media>? =
        mediaTemplate.opsForValue()
            .get(key(id))
            .awaitFirstOrNull()

    override suspend fun put(id: String, values: List<Media>, ttl: Duration) {
        val (sourceUrl, media) = id to values
        if (media.isEmpty()) return

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

    override suspend fun evict(id: String) {
        val sourceUrl = id
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

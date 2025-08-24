package com.download.downloaderbot.core.service

import com.download.downloaderbot.core.cache.MediaCache
import com.download.downloaderbot.core.domain.Media
import mu.KotlinLogging
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service
import java.time.Duration

private val log = KotlinLogging.logger {}

@Service
@Primary
class CachedMediaDownloadService(
    private val delegate: MediaDownloadService,
    private val cache: MediaCache
) : MediaDownloadService {

    // TODO: make ttl configurable
    private val ttl = Duration.ofDays(7)

    override suspend fun download(url: String): List<Media> {
        cache.get(url)?.let { cached ->
            log.info { "Cache hit for url=$url, returning ${cached.size} media item(s)" }
            return cached
        }

        val result = delegate.download(url)
        cache.put(result, ttl)
        return result
    }
}
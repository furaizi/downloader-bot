package com.download.downloaderbot.core.service

import com.download.downloaderbot.core.cache.MediaCache
import com.download.downloaderbot.core.config.properties.CacheProperties
import com.download.downloaderbot.core.domain.Media
import com.download.downloaderbot.core.downloader.DownloadInProgressException
import com.download.downloaderbot.core.lock.UrlLockManager
import kotlinx.coroutines.delay
import mu.KotlinLogging
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service
import java.time.Duration

private val log = KotlinLogging.logger {}

@Service
@Primary
class CachedMediaDownloadService(
    private val delegate: MediaDownloadService,
    private val mediaCache: MediaCache,
    private val props: CacheProperties,
    private val urlLock: UrlLockManager,
) : MediaDownloadService {

    override suspend fun download(url: String): List<Media> {
        getCachedOrNull(url)?.let { return it }

        var token = urlLock.tryAcquire(url, props.lockTtl)
        if (token == null) {
            waitForCache(url)?.let { return it }
            token = urlLock.tryAcquire(url, props.lockTtl)
                ?: throw DownloadInProgressException(url)
        }

        try {
            getCachedOrNull(url, afterLock = true)?.let { return it }
            val result = delegate.download(url)
            mediaCache.put(result, props.mediaTtl)
            return result
        } finally {
            urlLock.release(url, token)
        }
    }

    private suspend fun getCachedOrNull(url: String, afterLock: Boolean = false): List<Media>? {
        return mediaCache.get(url)?.also { cached ->
            val stage = if (afterLock) "after lock" else "before lock"
            log.info { "Cache hit $stage for url=$url, returning ${cached.size} media item(s)" }
        }
    }

    private suspend fun waitForCache(url: String): List<Media>? {
        val end = System.nanoTime() + props.waitTimeout.toNanos()
        val stepMs = props.waitTimeout.toMillis().coerceAtLeast(100)
        while (System.nanoTime() < end) {
            getCachedOrNull(url)?.let { return it }
            delay(stepMs)
        }
        return null
    }
}

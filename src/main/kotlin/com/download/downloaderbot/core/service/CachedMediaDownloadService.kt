package com.download.downloaderbot.core.service

import com.download.downloaderbot.core.cache.MediaCache
import com.download.downloaderbot.core.net.UrlNormalizer
import com.download.downloaderbot.core.config.properties.CacheProperties
import com.download.downloaderbot.core.domain.Media
import com.download.downloaderbot.core.downloader.DownloadInProgressException
import com.download.downloaderbot.core.downloader.UnsupportedSourceException
import com.download.downloaderbot.core.lock.UrlLockManager
import com.download.downloaderbot.core.net.UrlResolver
import com.download.downloaderbot.core.security.UrlAllowlist
import kotlinx.coroutines.delay
import mu.KotlinLogging
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

@Service
@Primary
class CachedMediaDownloadService(
    private val delegate: MediaDownloadService,
    private val mediaCache: MediaCache,
    private val props: CacheProperties,
    private val urlLock: UrlLockManager,
    private val urlResolver: UrlResolver,
    private val urlNormalizer: UrlNormalizer,
    private val allowlist: UrlAllowlist
) : MediaDownloadService {

    override suspend fun download(url: String): List<Media> {
        val resolvedUrl = urlResolver.finalUrl(url)
        val normalizedUrl = urlNormalizer.normalize(resolvedUrl)

        if (!allowlist.isAllowed(normalizedUrl))
            throw UnsupportedSourceException(normalizedUrl)

        getCachedOrNull(normalizedUrl)?.let { return it }

        var token = urlLock.tryAcquire(normalizedUrl, props.lockTtl)
        if (token == null) {
            waitForCache(normalizedUrl)?.let { return it }
            token = urlLock.tryAcquire(normalizedUrl, props.lockTtl)
                ?: throw DownloadInProgressException(normalizedUrl)
        }

        try {
            getCachedOrNull(normalizedUrl, afterLock = true)?.let { return it }
            val result = delegate.download(normalizedUrl)
            mediaCache.put(result, props.mediaTtl)
            return result
        } finally {
            urlLock.release(normalizedUrl, token)
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

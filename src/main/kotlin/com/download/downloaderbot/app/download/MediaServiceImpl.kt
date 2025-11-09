package com.download.downloaderbot.app.download

import com.download.downloaderbot.app.config.properties.CacheProperties
import com.download.downloaderbot.core.cache.CachePort
import com.download.downloaderbot.core.concurrency.DownloadSlots
import com.download.downloaderbot.core.domain.Media
import com.download.downloaderbot.core.downloader.DownloadInProgressException
import com.download.downloaderbot.core.downloader.MediaProvider
import com.download.downloaderbot.core.lock.UrlLockManager
import kotlinx.coroutines.delay
import mu.KotlinLogging
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

@Service
class MediaServiceImpl(
    private val provider: MediaProvider,
    private val urlOps: UrlOps,
    private val cache: CachePort<String, List<Media>>,
    private val cacheProps: CacheProperties,
    private val urlLock: UrlLockManager,
    private val slots: DownloadSlots,
) : MediaService {
    override suspend fun supports(url: String): Boolean {
        val finalUrl = urlOps.finalOf(url)
        return provider.supports(finalUrl)
    }

    override suspend fun download(url: String): List<Media> {
        val finalUrl = urlOps.finalOf(url)

        cache.getWithLog(finalUrl)?.let { return it }

        var token = urlLock.tryAcquire(finalUrl, cacheProps.lockTtl)
        if (token == null) {
            cache.awaitGet(finalUrl)?.let { return it }
            token = urlLock.tryAcquire(finalUrl, cacheProps.lockTtl)
                ?: throw DownloadInProgressException(finalUrl)
        }

        return try {
            slots.withSlotOrThrow(url) {
                provider.download(finalUrl)
            }
        } finally {
            urlLock.release(finalUrl, token)
        }
    }

    private suspend fun CachePort<String, List<Media>>.getWithLog(
        url: String,
        afterLock: Boolean = false,
    ): List<Media>? {
        return this.get(url)?.also { cached ->
            val stage = if (afterLock) "after lock" else "before lock"
            log.info { "Cache hit $stage for url=$url, returning ${cached.size} media item(s)" }
        }
    }

    private suspend fun CachePort<String, List<Media>>.awaitGet(url: String): List<Media>? {
        val end = System.nanoTime() + cacheProps.waitTimeout.toNanos()
        while (System.nanoTime() < end) {
            this.getWithLog(url)?.let { return it }
            delay(cacheProps.waitPoll.toMillis())
        }
        return null
    }
}

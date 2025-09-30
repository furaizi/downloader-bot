package com.download.downloaderbot.app.download

import com.download.downloaderbot.core.lock.UrlLockManager
import com.download.downloaderbot.core.cache.CachePort
import com.download.downloaderbot.core.concurrency.DownloadSlots
import com.download.downloaderbot.app.config.properties.CacheProperties
import com.download.downloaderbot.core.domain.Media
import com.download.downloaderbot.core.downloader.DownloadInProgressException
import com.download.downloaderbot.core.downloader.UnsupportedSourceException
import com.download.downloaderbot.core.net.FinalUrlResolver
import com.download.downloaderbot.core.security.UrlAllowlist
import kotlinx.coroutines.delay
import mu.KotlinLogging
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

@Component
@Order(10)
class ResolveNormalizeInterceptor(
    private val resolver: FinalUrlResolver,
    private val normalizer: UrlNormalizer
) : MediaInterceptor {
    override suspend fun invoke(url: String, next: Handler): List<Media> {
        val resolved = resolver.resolve(url)
        val final = normalizer.normalize(resolved)
        return next(final)
    }
}

@Component
@Order(20)
class AllowlistInterceptor(
    private val allowlist: UrlAllowlist
) : MediaInterceptor {
    override suspend fun invoke(url: String, next: Handler): List<Media> {
        if (!allowlist.isAllowed(url))
            throw UnsupportedSourceException(url)
        return next(url)
    }
}

@Component
@Order(30)
class CacheReadBeforeLockInterceptor(
    private val cache: CachePort<String, List<Media>>
) : MediaInterceptor {
    override suspend fun invoke(url: String, next: Handler): List<Media> {
        cache.getWithLog(url)?.let { return it }
        return next(url)
    }
}

@Component
@Order(40)
class LockWaitInterceptor(
    private val urlLock: UrlLockManager,
    private val cache: CachePort<String, List<Media>>,
    private val props: CacheProperties
) : MediaInterceptor {
    override suspend fun invoke(url: String, next: Handler): List<Media> {
        var token = urlLock.tryAcquire(url, props.lockTtl)
        if (token == null) {
            cache.awaitGet(url)?.let { return it }
            token = urlLock.tryAcquire(url, props.lockTtl)
                ?: throw DownloadInProgressException(url)
        }

        try {
            return next(url)
        } finally {
            urlLock.release(url, token)
        }
    }

    private suspend fun CachePort<String, List<Media>>.awaitGet(url: String): List<Media>? {
        val end = System.nanoTime() + props.waitTimeout.toNanos()
        val stepMs = props.waitTimeout.toMillis().coerceAtLeast(100)
        while (System.nanoTime() < end) {
            this.getWithLog(url)?.let { return it }
            delay(stepMs)
        }
        return null
    }
}

@Component
@Order(50)
class CacheWriteAfterDownloadInterceptor(
    private val cache: CachePort<String, List<Media>>,
    private val props: CacheProperties
) : MediaInterceptor {
    override suspend fun invoke(url: String, next: Handler): List<Media> {
        val res = next(url)
        cache.put(url, res, props.mediaTtl)
        return res
    }
}

@Component
@Order(60)
class SlotsInterceptor(private val slots: DownloadSlots) : MediaInterceptor {
    override suspend fun invoke(url: String, next: Handler): List<Media> =
        slots.withSlotOrThrow(url) {
            next(url)
        }
}


private suspend fun CachePort<String, List<Media>>.getWithLog(url: String, afterLock: Boolean = false): List<Media>? {
    return this.get(url)?.also { cached ->
        val stage = if (afterLock) "after lock" else "before lock"
        log.info { "Cache hit $stage for url=$url, returning ${cached.size} media item(s)" }
    }
}

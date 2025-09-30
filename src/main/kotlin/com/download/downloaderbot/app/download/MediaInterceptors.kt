package com.download.downloaderbot.app.download

import com.download.downloaderbot.core.concurrency.DownloadSlots
import com.download.downloaderbot.core.domain.Media
import com.download.downloaderbot.core.downloader.UnsupportedSourceException
import com.download.downloaderbot.core.net.FinalUrlResolver
import com.download.downloaderbot.core.security.UrlAllowlist
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
@Order(60)
class SlotsInterceptor(private val slots: DownloadSlots) : MediaInterceptor {
    override suspend fun invoke(url: String, next: Handler): List<Media> =
        slots.withSlotOrThrow(url) {
            next(url)
        }
}


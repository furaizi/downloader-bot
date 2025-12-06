package com.download.downloaderbot.app.download

import com.download.downloaderbot.app.config.properties.CacheProperties
import com.download.downloaderbot.core.cache.CachePort
import com.download.downloaderbot.core.domain.Media
import com.download.downloaderbot.core.downloader.DownloadInProgressException
import com.download.downloaderbot.core.downloader.MediaProvider
import com.download.downloaderbot.core.lock.UrlLockManager
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import java.time.Duration

// Real values here don't matter; we just need a consistent object.
private val DEFAULT_CACHE_PROPS = CacheProperties(schemaVersion = 1)

class MediaServiceImplTest : FunSpec({

    lateinit var provider: MediaProvider
    lateinit var urlOps: UrlOps
    lateinit var cache: CachePort<String, List<Media>>
    lateinit var urlLock: UrlLockManager

    fun service(cacheProps: CacheProperties = DEFAULT_CACHE_PROPS) = MediaServiceImpl(provider, urlOps, cache, cacheProps, urlLock)

    beforeTest {
        provider = mockk()
        urlOps = mockk()
        cache = mockk()
        urlLock = mockk()
    }

    test("supports delegates url through UrlOps to provider") {
        val rawUrl = " https://short.url "
        val finalUrl = "https://normalized.url"

        coEvery { urlOps.finalOf(rawUrl) } returns finalUrl
        coEvery { provider.supports(finalUrl) } returns true

        val result = service().supports(rawUrl)

        result shouldBe true

        coVerifyOrder {
            urlOps.finalOf(rawUrl)
            provider.supports(finalUrl)
        }
    }

    test("download returns cached value when present before lock") {
        val rawUrl = "https://example.com/video"
        val finalUrl = "https://normalized.example.com/video"
        val cached = listOf(mockk<Media>(), mockk())

        coEvery { urlOps.finalOf(rawUrl) } returns finalUrl
        coEvery { cache.get(finalUrl) } returns cached

        val result = service().download(rawUrl)

        result shouldContainExactly cached

        coVerify(exactly = 1) { cache.get(finalUrl) }
        coVerify(exactly = 0) { urlLock.tryAcquire(any(), any()) }
        coVerify(exactly = 0) { provider.download(any()) }
    }

    test("download acquires lock and downloads when cache is empty") {
        val rawUrl = "https://example.com/video"
        val finalUrl = "https://normalized.example.com/video"
        val token = "lock-token"
        val downloaded = listOf(mockk<Media>())

        coEvery { urlOps.finalOf(rawUrl) } returns finalUrl
        coEvery { cache.get(finalUrl) } returns null
        coEvery { urlLock.tryAcquire(finalUrl, DEFAULT_CACHE_PROPS.lockTtl) } returns token
        coEvery { provider.download(finalUrl) } returns downloaded
        coEvery { urlLock.release(finalUrl, token) } returns Unit

        val result = service().download(rawUrl)

        result shouldContainExactly downloaded

        coVerifyOrder {
            urlOps.finalOf(rawUrl)
            cache.get(finalUrl)
            urlLock.tryAcquire(finalUrl, DEFAULT_CACHE_PROPS.lockTtl)
            provider.download(finalUrl)
            urlLock.release(finalUrl, token)
        }
    }

    test("download waits for cache when initial lock acquisition fails") {
        val rawUrl = "https://example.com/video"
        val finalUrl = "https://normalized.example.com/video"
        val cachedAfterWait = listOf(mockk<Media>())

        coEvery { urlOps.finalOf(rawUrl) } returns finalUrl
        // 1st call to get: before the first tryAcquire (returns null)
        // 2nd call to get: during awaitGet (returns cached)
        coEvery { cache.get(finalUrl) } returnsMany listOf(null, cachedAfterWait)
        coEvery { urlLock.tryAcquire(finalUrl, DEFAULT_CACHE_PROPS.lockTtl) } returns null

        val result = service().download(rawUrl)

        result shouldContainExactly cachedAfterWait

        coVerify(exactly = 1) { urlLock.tryAcquire(finalUrl, DEFAULT_CACHE_PROPS.lockTtl) }
        coVerify(exactly = 2) { cache.get(finalUrl) }
        coVerify(exactly = 0) { provider.download(any()) }
        coVerify(exactly = 0) { urlLock.release(any(), any()) }
    }

    test("download acquires lock after waiting when cache did not fill") {
        val rawUrl = "https://example.com/video"
        val finalUrl = "https://normalized.example.com/video"
        val token = "lock-token"
        val downloaded = listOf(mockk<Media>())

        // To avoid waiting for the real timeout, make it zero.
        val shortWaitProps =
            CacheProperties(
                schemaVersion = 1,
                waitTimeout = Duration.ZERO,
                waitPoll = Duration.ZERO,
            )

        coEvery { urlOps.finalOf(rawUrl) } returns finalUrl
        coEvery { cache.get(finalUrl) } returns null
        coEvery { urlLock.tryAcquire(finalUrl, shortWaitProps.lockTtl) } returnsMany listOf(null, token)
        coEvery { provider.download(finalUrl) } returns downloaded
        coEvery { urlLock.release(finalUrl, token) } returns Unit

        val result = service(shortWaitProps).download(rawUrl)

        result shouldContainExactly downloaded

        coVerify(exactly = 2) { urlLock.tryAcquire(finalUrl, shortWaitProps.lockTtl) }
        coVerify(exactly = 1) { provider.download(finalUrl) }
        coVerify(exactly = 1) { urlLock.release(finalUrl, token) }
    }

    test("download throws DownloadInProgressException when lock cannot be acquired") {
        val rawUrl = "https://example.com/video"
        val finalUrl = "https://normalized.example.com/video"

        val shortWaitProps =
            CacheProperties(
                schemaVersion = 1,
                waitTimeout = Duration.ZERO,
                waitPoll = Duration.ZERO,
            )

        coEvery { urlOps.finalOf(rawUrl) } returns finalUrl
        coEvery { cache.get(finalUrl) } returns null
        coEvery { urlLock.tryAcquire(finalUrl, shortWaitProps.lockTtl) } returnsMany listOf(null, null)

        shouldThrow<DownloadInProgressException> {
            service(shortWaitProps).download(rawUrl)
        }

        coVerify(exactly = 2) { urlLock.tryAcquire(finalUrl, shortWaitProps.lockTtl) }
        coVerify(exactly = 0) { provider.download(any()) }
        coVerify(exactly = 0) { urlLock.release(any(), any()) }
    }

    test("download releases lock even when provider fails") {
        val rawUrl = "https://example.com/video"
        val finalUrl = "https://normalized.example.com/video"
        val token = "lock-token"

        coEvery { urlOps.finalOf(rawUrl) } returns finalUrl
        coEvery { cache.get(finalUrl) } returns null
        coEvery { urlLock.tryAcquire(finalUrl, DEFAULT_CACHE_PROPS.lockTtl) } returns token
        coEvery { provider.download(finalUrl) } throws IllegalStateException("boom")
        coEvery { urlLock.release(finalUrl, token) } returns Unit

        shouldThrow<IllegalStateException> {
            service().download(rawUrl)
        }

        coVerify(exactly = 1) { urlLock.tryAcquire(finalUrl, DEFAULT_CACHE_PROPS.lockTtl) }
        coVerify(exactly = 1) { urlLock.release(finalUrl, token) }
    }
})

package com.download.downloaderbot.app.download

import com.download.downloaderbot.app.config.properties.CacheProperties
import com.download.downloaderbot.core.cache.CachePort
import com.download.downloaderbot.core.domain.Media
import com.download.downloaderbot.core.domain.MediaType
import com.download.downloaderbot.core.downloader.DownloadInProgressException
import com.download.downloaderbot.core.downloader.MediaProvider
import com.download.downloaderbot.core.lock.UrlLockManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Duration
import java.time.OffsetDateTime

class MediaServiceImplTest {
    private val testUrl = "https://youtube.com/watch?v=abc"
    private val normalizedUrl = "https://youtube.com/watch?v=abc"

    private val testMedia =
        listOf(
            Media(
                type = MediaType.VIDEO,
                fileUrl = "/tmp/video.mp4",
                sourceUrl = testUrl,
                title = "Test Video",
                downloadedAt = OffsetDateTime.now(),
            ),
        )

    @Test
    fun `supports delegates to provider after url normalization`() =
        runTest {
            val provider =
                mockk<MediaProvider> {
                    coEvery { supports(normalizedUrl) } returns true
                }
            val service = createService(provider = provider)

            assertTrue(service.supports(testUrl))
            coVerify { provider.supports(normalizedUrl) }
        }

    @Test
    fun `supports returns false when provider does not support`() =
        runTest {
            val provider =
                mockk<MediaProvider> {
                    coEvery { supports(any()) } returns false
                }
            val service = createService(provider = provider)

            assertFalse(service.supports(testUrl))
        }

    @Test
    fun `download returns cached media on cache hit`() =
        runTest {
            val cache = mockk<CachePort<String, List<Media>>>()
            coEvery { cache.get(any()) } returns testMedia

            val provider = mockk<MediaProvider>()
            val service = createService(provider = provider, cache = cache)

            val result = service.download(testUrl)

            assertEquals(testMedia, result)
            coVerify(exactly = 0) { provider.download(any()) }
        }

    @Test
    fun `download acquires lock and downloads on cache miss`() =
        runTest {
            val lockToken = "lock-token-123"
            val cache = mockk<CachePort<String, List<Media>>>()
            coEvery { cache.get(any()) } returns null

            val provider =
                mockk<MediaProvider> {
                    coEvery { download(normalizedUrl) } returns testMedia
                }
            val urlLock =
                mockk<UrlLockManager> {
                    coEvery { tryAcquire(normalizedUrl, any()) } returns lockToken
                    coEvery { release(normalizedUrl, lockToken) } returns Unit
                }

            val service = createService(provider = provider, cache = cache, urlLock = urlLock)

            val result = service.download(testUrl)

            assertEquals(testMedia, result)
            coVerify { urlLock.tryAcquire(normalizedUrl, any()) }
            coVerify { provider.download(normalizedUrl) }
            coVerify { urlLock.release(normalizedUrl, lockToken) }
        }

    @Test
    fun `download releases lock even on provider exception`() =
        runTest {
            val lockToken = "lock-token-123"
            val cache = mockk<CachePort<String, List<Media>>>()
            coEvery { cache.get(any()) } returns null

            val provider =
                mockk<MediaProvider> {
                    coEvery { download(any()) } throws RuntimeException("Download failed")
                }
            val urlLock =
                mockk<UrlLockManager> {
                    coEvery { tryAcquire(any(), any()) } returns lockToken
                    coEvery { release(any(), any()) } returns Unit
                }

            val service = createService(provider = provider, cache = cache, urlLock = urlLock)

            assertThrows<RuntimeException> {
                service.download(testUrl)
            }

            coVerify { urlLock.release(normalizedUrl, lockToken) }
        }

    @Test
    fun `download throws DownloadInProgressException when lock unavailable and no cache after wait`() =
        runTest {
            val cache = mockk<CachePort<String, List<Media>>>()
            coEvery { cache.get(any()) } returns null

            val urlLock =
                mockk<UrlLockManager> {
                    coEvery { tryAcquire(any(), any()) } returns null
                }
            val cacheProps =
                CacheProperties(
                    schemaVersion = 1,
                    waitTimeout = Duration.ofMillis(50),
                    waitPoll = Duration.ofMillis(10),
                )

            val service = createService(cache = cache, urlLock = urlLock, cacheProps = cacheProps)

            assertThrows<DownloadInProgressException> {
                service.download(testUrl)
            }
        }

    private fun createService(
        provider: MediaProvider =
            mockk {
                coEvery { supports(any()) } returns true
                coEvery { download(any()) } returns testMedia
            },
        urlOps: UrlOps =
            mockk {
                coEvery { finalOf(any()) } answers { firstArg() }
            },
        cache: CachePort<String, List<Media>> =
            mockk<CachePort<String, List<Media>>>().also {
                coEvery { it.get(any()) } returns null
            },
        cacheProps: CacheProperties =
            CacheProperties(
                schemaVersion = 1,
                lockTtl = Duration.ofSeconds(60),
                waitTimeout = Duration.ofMillis(50),
                waitPoll = Duration.ofMillis(10),
            ),
        urlLock: UrlLockManager =
            mockk {
                coEvery { tryAcquire(any(), any()) } returns "token"
                coEvery { release(any(), any()) } returns Unit
            },
    ): MediaServiceImpl {
        return MediaServiceImpl(
            provider = provider,
            urlOps = urlOps,
            cache = cache,
            cacheProps = cacheProps,
            urlLock = urlLock,
        )
    }
}

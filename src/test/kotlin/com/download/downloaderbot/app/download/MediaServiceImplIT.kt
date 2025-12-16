package com.download.downloaderbot.app.download

import com.download.downloaderbot.app.config.properties.CacheProperties
import com.download.downloaderbot.bot.config.BotTestConfig
import com.download.downloaderbot.bot.config.MediaServiceTestConfig
import com.download.downloaderbot.bot.config.RedisTestConfig
import com.download.downloaderbot.core.cache.CachePort
import com.download.downloaderbot.core.domain.Media
import com.download.downloaderbot.core.domain.MediaType
import com.download.downloaderbot.core.downloader.MediaProvider
import com.download.downloaderbot.core.lock.UrlLockManager
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.OffsetDateTime

@Import(BotTestConfig::class, RedisTestConfig::class, MediaServiceTestConfig::class)
@Testcontainers
@ActiveProfiles("test")
@SpringBootTest(properties = ["spring.config.location=classpath:/"])
class MediaServiceImplIT(
    private val mediaService: MediaServiceImpl,
    private val mediaProvider: MediaProvider,
    private val cache: CachePort<String, List<Media>>,
    private val cacheProps: CacheProperties,
    private val normalizer: UrlNormalizer,
    private val urlLock: UrlLockManager,
) : FunSpec({

        extension(SpringExtension)

        beforeTest {
            clearMocks(mediaProvider)
        }

        fun media(id: Int) =
            listOf(
                Media(
                    type = MediaType.VIDEO,
                    fileUrl = "file-$id.mp4",
                    sourceUrl = "https://example.com/$id",
                    title = "Video $id",
                    fileUniqueId = "unique-$id",
                    lastFileId = "fileId-$id",
                    downloadedAt = OffsetDateTime.parse("2024-01-01T00:00:00Z"),
                ),
            )

        test("supports delegates to MediaProvider with normalized url") {
            val rawUrl = " https://example.com/watch?v=123&utm_source=foo "
            val finalUrl = normalizer.normalize(rawUrl)

            coEvery { mediaProvider.supports(any()) } returns true

            val supported = mediaService.supports(rawUrl)

            supported shouldBe true
            coVerify(exactly = 1) { mediaProvider.supports(finalUrl) }
        }

        test("download returns cached media when cache hit occurs before lock acquisition") {
            val rawUrl = "https://example.com/video"
            val finalUrl = normalizer.normalize(rawUrl)
            val cached = media(id = 1)

            cache.put(finalUrl, cached, cacheProps.mediaTtl)

            val result = mediaService.download(rawUrl)

            result shouldBe cached
            coVerify(exactly = 0) { mediaProvider.download(any()) }
        }

        test("download returns cached media when another process holds the lock and cache is populated during wait") {
            val rawUrl = "https://example.com/concurrent"
            val finalUrl = normalizer.normalize(rawUrl)
            val cached = media(id = 2)

            val token = urlLock.tryAcquire(finalUrl, cacheProps.lockTtl)
            token.shouldNotBeNull()

            coEvery { mediaProvider.download(any()) } returns media(id = 42)

            val job =
                async {
                    mediaService.download(rawUrl)
                }

            delay(100)
            cache.put(finalUrl, cached, cacheProps.mediaTtl)

            val result = job.await()

            result shouldBe cached
            coVerify(exactly = 0) { mediaProvider.download(any()) }
        }

        test("download acquires lock, delegates to MediaProvider and releases lock on cache miss") {
            val rawUrl = "https://example.com/miss"
            val finalUrl = normalizer.normalize(rawUrl)
            val fromProvider = media(id = 5)

            coEvery { mediaProvider.download(finalUrl) } returns fromProvider
            coEvery { mediaProvider.supports(any()) } returns true

            val result = mediaService.download(rawUrl)

            result shouldBe fromProvider
            coVerify(exactly = 1) { mediaProvider.download(finalUrl) }

            val secondToken = urlLock.tryAcquire(finalUrl, cacheProps.lockTtl)
            secondToken shouldNotBe null
        }
    })

package com.download.downloaderbot.infra.lock

import com.download.downloaderbot.app.config.properties.CacheProperties
import com.download.downloaderbot.app.download.MediaServiceImpl
import com.download.downloaderbot.app.download.UrlNormalizer
import com.download.downloaderbot.bot.config.BotTestConfig
import com.download.downloaderbot.core.cache.CachePort
import com.download.downloaderbot.core.domain.Media
import com.download.downloaderbot.core.domain.MediaType
import com.download.downloaderbot.core.downloader.MediaProvider
import com.download.downloaderbot.infra.config.MediaServiceTestConfig
import com.download.downloaderbot.infra.config.RedisTestConfig
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.collections.shouldContainOnly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory
import org.springframework.test.context.ActiveProfiles
import java.time.OffsetDateTime

@Import(BotTestConfig::class, RedisTestConfig::class, MediaServiceTestConfig::class)
@ActiveProfiles("test")
@SpringBootTest(properties = ["spring.config.location=classpath:/"])
class RedisUrlLockThunderingHerdIT
    @Autowired
    constructor(
        private val mediaService: MediaServiceImpl,
        private val mediaProvider: MediaProvider,
        private val cache: CachePort<String, List<Media>>,
        private val cacheProps: CacheProperties,
        private val normalizer: UrlNormalizer,
        private val factory: ReactiveRedisConnectionFactory,
    ) : FunSpec({

            extension(SpringExtension)

            beforeTest {
                clearMocks(mediaProvider)
                factory.reactiveConnection
                    .serverCommands()
                    .flushDb()
                    .awaitFirstOrNull()
            }

            test("thundering herd: only one download happens for burst of identical urls") {
                val rawUrl = "https://viral.example.com/watch?v=abc&utm_source=spam&utm_medium=telegram"
                val finalUrl = normalizer.normalize(rawUrl)
                val media =
                    listOf(
                        Media(
                            type = MediaType.VIDEO,
                            fileUrl = "/tmp/viral.mp4",
                            sourceUrl = finalUrl,
                            title = "Viral video",
                            fileUniqueId = "uniq-1",
                            downloadedAt = OffsetDateTime.parse("2024-01-01T00:00:00Z"),
                        ),
                    )

                coEvery { mediaProvider.download(finalUrl) } coAnswers {
                    delay(200)
                    cache.put(finalUrl, media, cacheProps.mediaTtl)
                    media
                }

                val results =
                    coroutineScope {
                        (1..50).map {
                            async(Dispatchers.Default) {
                                mediaService.download(rawUrl)
                            }
                        }.awaitAll()
                    }

                results.shouldHaveSize(50)
                results.toSet().shouldHaveSize(1)
                results.first() shouldBe media
                cache.get(finalUrl) shouldBe media
                coVerify(exactly = 1) { mediaProvider.download(finalUrl) }
            }
        })

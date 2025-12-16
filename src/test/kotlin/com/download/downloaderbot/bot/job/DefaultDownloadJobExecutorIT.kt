package com.download.downloaderbot.bot.job

import com.download.downloaderbot.app.download.StubMediaService
import com.download.downloaderbot.bot.config.BotTestConfig
import com.download.downloaderbot.bot.config.DownloadJobExecutorTestConfig
import com.download.downloaderbot.bot.config.RedisTestConfig
import com.download.downloaderbot.bot.config.properties.BotProperties
import com.download.downloaderbot.bot.gateway.RecordingBotPort
import com.download.downloaderbot.bot.gateway.telegram.fileId
import com.download.downloaderbot.core.cache.CachePort
import com.download.downloaderbot.core.domain.Media
import com.download.downloaderbot.core.domain.MediaType
import com.download.downloaderbot.core.downloader.MediaNotFoundException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest(
    classes = [
        BotTestConfig::class,
        DownloadJobExecutorTestConfig::class,
        RedisTestConfig::class,
    ],
    properties = ["spring.config.location=classpath:/"],
)
@ActiveProfiles("test")
class DefaultDownloadJobExecutorIT
    @Autowired
    constructor(
        private val executor: DefaultDownloadJobExecutor,
        private val mediaService: StubMediaService,
        private val botPort: RecordingBotPort,
        private val cache: CachePort<String, List<Media>>,
        private val botProps: BotProperties,
    ) : FunSpec({

            extension(SpringExtension)

            beforeTest {
                mediaService.reset()
                botPort.reset()
            }

            fun downloadJob(
                url: String,
                chatId: Long,
            ) = DownloadJob(
                sourceUrl = url,
                chatId = chatId,
                replyToMessageId = null,
                commandContext = mockk(relaxed = true),
            )

            test("sends single video with promo and updates cache") {
                val url = "https://example.com/video"
                val chatId = 100L

                mediaService.stubDownload(
                    url,
                    listOf(
                        Media(
                            MediaType.VIDEO,
                            "/tmp/video.mp4",
                            url,
                            "Video",
                        ),
                    ),
                )

                val job = downloadJob(url, chatId)
                executor.execute(job)

                botPort.sentMedia.shouldHaveSize(1)
                botPort.sentTexts.shouldBeEmpty()

                val sent = botPort.sentMedia.single()
                sent.options.caption shouldBe botProps.promoText
                sent.options.replyMarkup.shouldNotBeNull()

                val cached = cache.get(url)
                cached.shouldNotBeNull()
                cached.first().lastFileId shouldBe sent.message.fileId
            }

            test("sends image album and promo message") {
                val url = "https://example.com/album"
                val chatId = 200L

                mediaService.stubDownload(
                    url,
                    listOf(
                        Media(
                            MediaType.IMAGE,
                            "/tmp/1.jpg",
                            url,
                            "1",
                        ),
                        Media(
                            MediaType.IMAGE,
                            "/tmp/2.jpg",
                            url,
                            "2",
                        ),
                    ),
                )

                val job = downloadJob(url, chatId)
                executor.execute(job)

                botPort.sentAlbums.shouldHaveSize(1)
                botPort.sentTexts.shouldHaveSize(1)

                val cached = cache.get(url)
                cached.shouldNotBeNull()
                cached.size shouldBe 2
            }

            test("throws when media list is empty and does not touch bot or cache") {
                val url = "https://example.com/empty"
                val chatId = 300L

                mediaService.stubDownload(url, emptyList())

                val job = downloadJob(url, chatId)
                shouldThrow<MediaNotFoundException> { executor.execute(job) }

                botPort.sentMedia.shouldHaveSize(0)
                botPort.sentAlbums.shouldHaveSize(0)
                botPort.sentChunkedAlbums.shouldHaveSize(0)
                botPort.sentTexts.shouldHaveSize(0)

                cache.get(url).shouldBeNull()
            }
        })

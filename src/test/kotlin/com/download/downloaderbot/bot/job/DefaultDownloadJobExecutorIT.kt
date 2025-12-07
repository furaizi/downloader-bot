package com.download.downloaderbot.bot.job

import com.download.downloaderbot.app.download.StubMediaService
import com.download.downloaderbot.bot.config.DownloadJobExecutorTestConfig
import com.download.downloaderbot.bot.config.TestBotConfig
import com.download.downloaderbot.bot.config.properties.BotProperties
import com.download.downloaderbot.bot.gateway.RecordingBotPort
import com.download.downloaderbot.bot.gateway.telegram.fileId
import com.download.downloaderbot.core.domain.Media
import com.download.downloaderbot.core.domain.MediaType
import com.download.downloaderbot.infra.cache.InMemoryMediaCacheAdapter
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles

@Import(TestBotConfig::class, DownloadJobExecutorTestConfig::class)
@SpringBootTest
@ActiveProfiles("test")
class DefaultDownloadJobExecutorIT : FunSpec() {

    @Autowired
    lateinit var executor: DefaultDownloadJobExecutor

    @Autowired
    lateinit var mediaService: StubMediaService

    @Autowired
    lateinit var botPort: RecordingBotPort

    @Autowired
    lateinit var cache: InMemoryMediaCacheAdapter

    @Autowired
    lateinit var botProps: BotProperties

    override fun extensions() = listOf(SpringExtension)

    init {

        beforeTest {
            mediaService.reset()
            botPort.reset()
            cache.clear()
        }

        test("sends single video with promo and updates cache") {
            val url = "https://example.com/video"
            val chatId = 100L

            mediaService.stubDownload(
                url,
                listOf(
                    Media(
                        type = MediaType.VIDEO,
                        fileUrl = "/tmp/video.mp4",
                        sourceUrl = url,
                        title = "Video",
                    ),
                ),
            )

            val job =
                DownloadJob(
                    sourceUrl = url,
                    chatId = chatId,
                    replyToMessageId = null,
                    commandContext = mockk(relaxed = true),
                )

            executor.execute(job)

            botPort.sentMedia.shouldHaveSize(1)
            botPort.sentTexts.shouldBeEmpty()

            val sent = botPort.sentMedia.single()
            sent.options.caption shouldBe botProps.promoText
            sent.options.replyMarkup.shouldNotBeNull()

            cache.get(url)!!
                .first().lastFileId shouldBe sent.message.fileId
        }

        test("sends image album and promo message") {
            val url = "https://example.com/album"
            val chatId = 200L

            mediaService.stubDownload(
                url,
                listOf(
                    Media(
                        type = MediaType.IMAGE,
                        fileUrl = "/tmp/1.jpg",
                        sourceUrl = url,
                        title = "1",
                    ),
                    Media(
                        type = MediaType.IMAGE,
                        fileUrl = "/tmp/2.jpg",
                        sourceUrl = url,
                        title = "2",
                    ),
                ),
            )

            val job =
                DownloadJob(
                    sourceUrl = url,
                    chatId = chatId,
                    replyToMessageId = null,
                    commandContext = mockk(relaxed = true),
                )

            executor.execute(job)

            botPort.sentAlbums.shouldHaveSize(1)
            botPort.sentTexts.shouldHaveSize(1)

            cache.get(url)!!.size shouldBe 2
        }
    }
}

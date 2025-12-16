package com.download.downloaderbot.e2e

import com.download.downloaderbot.app.config.properties.MediaProperties
import com.download.downloaderbot.app.download.UrlNormalizer
import com.download.downloaderbot.bot.commands.util.updateDownload
import com.download.downloaderbot.bot.config.BotTestConfig
import com.download.downloaderbot.infra.config.RedisTestConfig
import com.download.downloaderbot.bot.config.properties.BotProperties
import com.download.downloaderbot.bot.core.UpdateHandler
import com.download.downloaderbot.bot.gateway.InputFile
import com.download.downloaderbot.bot.gateway.RecordingBotPort
import com.download.downloaderbot.bot.gateway.telegram.fileId
import com.download.downloaderbot.core.cache.CachePort
import com.download.downloaderbot.core.domain.Media
import com.download.downloaderbot.core.domain.MediaType
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.file.shouldExist
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.paths.shouldExist
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.junit.jupiter.Testcontainers
import java.nio.file.Files
import java.nio.file.Path
import kotlin.time.Duration.Companion.seconds

@SpringBootTest(
    classes = [
        BotTestConfig::class,
        RedisTestConfig::class,
        DownloadHappyPathTestConfig::class,
    ],
    properties = [
        "spring.config.location=classpath:/",
    ],
)
@ActiveProfiles("test")
@Testcontainers
class DownloadHappyPathE2E(
    private val updateHandler: UpdateHandler,
    private val botPort: RecordingBotPort,
    private val cache: CachePort<String, List<Media>>,
    private val normalizer: UrlNormalizer,
    private val botProps: BotProperties,
    private val mediaProps: MediaProperties,
) : FunSpec({

        extension(SpringExtension)

        beforeTest {
            botPort.reset()
            mediaProps.basePath.toFile().deleteRecursively()
            Files.createDirectories(mediaProps.basePath)
        }

        test("downloads video via /download command and stores cache") {
            val url = "https://example.com/magic/video-123"
            val chatId = 4242L
            val messageId = 111L
            val key = normalizer.normalize(url)
            cache.evict(key)

            val update = updateDownload(url, chatId, messageId)
            updateHandler.handle(update)

            val sent =
                eventually(5.seconds) {
                    botPort.sentMedia.shouldHaveSize(1)
                    botPort.sentMedia.single()
                }

            sent.type shouldBe MediaType.VIDEO
            assertSoftly(sent.options) {
                caption shouldBe botProps.promoText
                replyMarkup shouldNotBe null
                replyToMessageId shouldBe messageId
            }

            val file = (sent.file as InputFile.Local).file
            file.shouldExist()

            val cached = eventually(5.seconds) {
                assertSoftly(cache.get(key)) {
                    this.shouldNotBeNull()
                    this.shouldHaveSize(1)
                } as List<Media>
            }

            val cachedMedia = cached.single()
            cachedMedia.lastFileId shouldBe sent.message.fileId
            Path.of(cachedMedia.fileUrl).shouldExist()
        }

        afterSpec {
            mediaDir.toFile().deleteRecursively()
        }

    }) {
    companion object {
        private val mediaDir: Path = Files.createTempDirectory("downloader-bot-media-")

        @JvmStatic
        @DynamicPropertySource
        fun props(registry: DynamicPropertyRegistry) {
            registry.add("downloader.media.base-dir") { mediaDir.toString() }
        }
    }
}

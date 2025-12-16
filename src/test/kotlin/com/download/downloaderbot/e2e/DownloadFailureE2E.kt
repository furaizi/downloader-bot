package com.download.downloaderbot.e2e

import com.download.downloaderbot.bot.commands.CommandContext
import com.download.downloaderbot.bot.commands.util.updateDownload
import com.download.downloaderbot.bot.config.BotTestConfig
import com.download.downloaderbot.bot.core.UpdateHandler
import com.download.downloaderbot.bot.exception.BotErrorGuard
import com.download.downloaderbot.bot.gateway.RecordingBotPort
import com.download.downloaderbot.app.config.properties.MediaProperties
import com.download.downloaderbot.infra.config.RedisTestConfig
import com.download.downloaderbot.core.lock.UrlLockManager
import com.download.downloaderbot.app.download.UrlNormalizer
import com.download.downloaderbot.core.cache.CachePort
import com.download.downloaderbot.core.domain.Media
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.nulls.shouldNotBeNull
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.junit.jupiter.Testcontainers
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import kotlin.time.Duration.Companion.seconds

@SpringBootTest(
    classes = [
        BotTestConfig::class,
        RedisTestConfig::class,
        DownloadFailureTestConfig::class,
    ],
    properties = [
        "spring.config.location=classpath:/",
    ],
)
@ActiveProfiles("test")
@Testcontainers
class DownloadFailureE2E(
    private val updateHandler: UpdateHandler,
    private val botPort: RecordingBotPort,
    private val errorGuard: BotErrorGuard,
    private val urlLock: UrlLockManager,
    private val mediaProps: MediaProperties,
    private val cache: CachePort<String, List<Media>>,
    private val normalizer: UrlNormalizer,
) : FunSpec({

        extension(SpringExtension)

        suspend fun send(
            messageId: Long,
            chatId: Long,
            url: String,
            updateId: Long = messageId,
        ) {
            val update = updateDownload(url, chatId, messageId, updateId)
            val args = update.message
                ?.text
                ?.trim()
                ?.split("\\s+".toRegex())
                ?: emptyList()
            val ctx = CommandContext(update, args)
            errorGuard.runSafely(ctx) {
                updateHandler.handle(update)
            }
        }

        beforeTest {
            botPort.reset()
            mediaProps.basePath.toFile().deleteRecursively()
            Files.createDirectories(mediaProps.basePath)
        }

        test("releases url lock and notifies user when download fails") {
            val url = "https://example.com/magic/fail"
            val chatId = 303L
            val firstMessageId = 1L
            val failureText = "Внутрішній інструмент не зміг виконатися."
            val key = normalizer.normalize(url)

            cache.evict(key)

            urlLock.tryAcquire(url, Duration.ofSeconds(1))?.let { urlLock.release(url, it) }

            send(firstMessageId, chatId, url)

            eventually(5.seconds) {
                botPort.sentTexts.shouldHaveSize(1)
                assertSoftly(botPort.sentTexts.first()) {
                    this.chatId shouldBe chatId
                    replyToMessageId shouldBe firstMessageId
                    text shouldBe failureText
                }
            }

            eventually(2.seconds) {
                val token = urlLock.tryAcquire(url, Duration.ofSeconds(1)).shouldNotBeNull()
                urlLock.release(url, token)
                cache.get(key) shouldBe null
            }

            val secondMessageId = 2L
            send(secondMessageId, chatId, url)

            eventually(5.seconds) {
                botPort.sentTexts.shouldHaveSize(2)
            }

            assertSoftly(botPort.sentTexts.last()) {
                this.chatId shouldBe chatId
                replyToMessageId shouldBe secondMessageId
                text shouldBe failureText
            }
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

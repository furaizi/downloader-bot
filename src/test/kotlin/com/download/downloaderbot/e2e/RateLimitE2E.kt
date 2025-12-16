package com.download.downloaderbot.e2e

import com.download.downloaderbot.bot.commands.CommandContext
import com.download.downloaderbot.bot.commands.util.updateText
import com.download.downloaderbot.bot.config.BotTestConfig
import com.download.downloaderbot.bot.core.UpdateHandler
import com.download.downloaderbot.bot.exception.BotErrorGuard
import com.download.downloaderbot.bot.gateway.RecordingBotPort
import com.download.downloaderbot.infra.config.RedisTestConfig
import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.lettuce.core.api.StatefulRedisConnection
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.junit.jupiter.Testcontainers
import java.nio.file.Files
import java.nio.file.Path

@SpringBootTest(
    classes = [
        BotTestConfig::class,
        RedisTestConfig::class,
        DownloadHappyPathTestConfig::class,
    ],
    properties = [
        "spring.config.location=classpath:/",
        "downloader.ratelimit.enabled=true",
        "downloader.ratelimit.namespace=rl-e2e",
        "downloader.ratelimit.chat.refill.period=5s",
        "downloader.ratelimit.group.refill.period=5s",
    ],
)
@ActiveProfiles("test")
@Testcontainers
class RateLimitE2E(
    private val updateHandler: UpdateHandler,
    private val botPort: RecordingBotPort,
    private val errorGuard: BotErrorGuard,
    private val redisConnection: StatefulRedisConnection<String, ByteArray>,
) : FunSpec({

        extension(SpringExtension)

        beforeTest {
            botPort.reset()
            redisConnection.sync().flushall()
            Files.createDirectories(mediaDir)
        }

        test("rejects messages beyond chat limit and notifies the user") {
            val userChatId = 101L
            val updates =
                (1L..5L).map { idx ->
                    updateText(
                        text = "/start",
                        chatId = userChatId,
                        messageId = idx,
                        updateId = idx,
                    )
                }

            updates.forEach { update ->
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

            botPort.sentTexts shouldHaveSize 5

            val welcome = "Привіт! Надішли мені посилання - я завантажу і відправлю відео."
            val throttled = "Занадто багато запитів, спробуйте пізніше."

            botPort.sentTexts.take(2).forEach { sent ->
                assertSoftly(sent) {
                    chatId shouldBe userChatId
                    replyToMessageId shouldBe null
                    text shouldBe welcome
                }
            }

            botPort.sentTexts.drop(2).forEachIndexed { idx, sent ->
                assertSoftly(sent) {
                    chatId shouldBe userChatId
                    replyToMessageId shouldBe updates[idx + 2].message?.messageId
                    text shouldBe throttled
                }
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

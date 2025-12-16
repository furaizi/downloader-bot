package com.download.downloaderbot.e2e.ratelimit

import com.download.downloaderbot.app.config.properties.MediaProperties
import com.download.downloaderbot.bot.commands.util.updateText
import com.download.downloaderbot.bot.core.UpdateHandler
import com.download.downloaderbot.bot.exception.BotErrorGuard
import com.download.downloaderbot.bot.gateway.RecordingBotPort
import com.download.downloaderbot.e2e.config.AbstractE2E
import com.download.downloaderbot.e2e.config.DownloaderBotE2E
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.lettuce.core.api.StatefulRedisConnection
import org.springframework.test.context.TestPropertySource

@DownloaderBotE2E
@TestPropertySource(
    properties = [
        "downloader.yt-dlp.runner=fake",
        "downloader.ratelimit.enabled=true",
        "downloader.ratelimit.namespace=rl-e2e",
        "downloader.ratelimit.chat.refill.period=5s",
        "downloader.ratelimit.group.refill.period=5s",
    ],
)
class RateLimitE2E(
    updateHandler: UpdateHandler,
    botPort: RecordingBotPort,
    mediaProps: MediaProperties,
    errorGuard: BotErrorGuard,
    private val redisConnection: StatefulRedisConnection<String, ByteArray>,
) : AbstractE2E(
        updateHandler,
        botPort,
        mediaProps,
        errorGuard,
        body = {

            beforeTest {
                redisConnection.sync().flushall()
            }

            test("rejects messages beyond chat limit and notifies the user") {
                val userChatId = 101L
                val updates =
                    (1L..5L).map { idx ->
                        updateText("/start", userChatId, idx, idx)
                    }

                updates.forEach { handle(it) }

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
        },
    )
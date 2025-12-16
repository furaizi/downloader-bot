package com.download.downloaderbot.bot.exception

import com.download.downloaderbot.bot.commands.util.ctx
import com.download.downloaderbot.bot.gateway.RecordingBotPort
import com.download.downloaderbot.core.downloader.*
import com.download.downloaderbot.infra.metrics.BotMetrics
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CancellationException
import java.time.Duration

class GlobalTelegramExceptionHandlerTest : FunSpec({

    val metrics = mockk<BotMetrics>(relaxed = true)
    val botPort = RecordingBotPort()
    val handler = GlobalTelegramExceptionHandler(botPort, metrics)

    beforeTest {
        botPort.sentTexts.clear()
        clearMocks(metrics)
    }

    test("should rethrow CancellationException without side effects") {
        val ctx = ctx(chatId = 123, replyToMessageId = 77)
        val ex = CancellationException("cancel")

        shouldThrow<CancellationException> {
            handler.handle(ex, ctx)
        }

        verify(exactly = 0) { metrics.errors.increment() }
        botPort.sentTexts shouldHaveSize 0
    }

    context("Exception mapping to user messages") {
        data class TestCase(
            val exception: Exception,
            val expectedText: String,
            val testName: String = exception::class.simpleName ?: "Unknown"
        )

        val testCases = listOf(
            TestCase(
                UnsupportedSourceException("http://src"),
                "Це джерело не підтримується."
            ),
            TestCase(
                MediaTooLargeException("http://src", 200 * 1024 * 1024, 100 * 1024 * 1024),
                "Медіафайл занадто великий. Обмеження: 100 МБ."
            ),
            TestCase(
                MediaNotFoundException("http://src"),
                "Нічого не знайдено за цим URL."
            ),
            TestCase(
                ToolExecutionException("ffmpeg", 1, "err"),
                "Внутрішній інструмент не зміг виконатися."
            ),
            TestCase(
                ToolTimeoutException("yt-dlp", Duration.ofSeconds(5), "out"),
                "Внутрішній інструмент перевищив час очікування: PT5S."
            ),
            TestCase(
                MediaDownloaderToolException("Internal error"),
                "Сталася внутрішня помилка інструменту."
            ),
            TestCase(
                DownloadInProgressException("http://src"),
                "Цей медіафайл уже завантажується, будь ласка, зачекайте."
            ),
            TestCase(
                BusyException("http://src"),
                "Завантажувач зараз зайнятий, спробуйте пізніше."
            ),
            TestCase(
                TooManyRequestsException("private", 123L),
                "Занадто багато запитів, спробуйте пізніше."
            ),
            TestCase(
                MediaDownloaderException("http://src", "General error"),
                "Сталася помилка під час обробки медіафайлу."
            ),
            TestCase(
                IllegalStateException("Unexpected boom"),
                "Сталася непередбачена помилка.",
                testName = "Unknown Exception (Default)"
            )
        )

        testCases.forEach { (ex, expectedText, name) ->
            test("handles $name correctly") {
                val chatId = 100L
                val replyId = 500L
                val ctx = ctx(chatId = chatId, replyToMessageId = replyId)

                handler.handle(ex, ctx)

                verify(exactly = 1) { metrics.errors.increment() }

                botPort.sentTexts shouldHaveSize 1
                assertSoftly(botPort.sentTexts.first()) {
                    this.chatId shouldBe chatId
                    this.replyToMessageId shouldBe replyId
                    this.text shouldBe expectedText
                }
            }
        }
    }
})
package com.download.downloaderbot.bot.commands

import com.download.downloaderbot.app.download.MediaService
import com.download.downloaderbot.bot.commands.util.InputValidator
import com.download.downloaderbot.bot.commands.util.ctx
import com.download.downloaderbot.bot.gateway.RecordingBotPort
import com.download.downloaderbot.bot.job.DownloadJob
import com.download.downloaderbot.bot.job.DownloadJobQueue
import com.download.downloaderbot.bot.ratelimit.guard.NoopRateLimitGuard
import com.download.downloaderbot.bot.ratelimit.guard.RejectAllRateLimitGuard
import com.download.downloaderbot.core.downloader.TooManyRequestsException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.CapturingSlot
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot

class DownloadCommandTest : FunSpec({

    val validator = InputValidator()

    lateinit var service: MediaService
    lateinit var bot: RecordingBotPort
    lateinit var queue: DownloadJobQueue
    lateinit var jobSlot: CapturingSlot<DownloadJob>

    beforeTest {
        service = mockk(relaxed = true)
        bot = RecordingBotPort()

        jobSlot = slot()
        queue = mockk()
        coEvery { queue.submit(capture(jobSlot)) } just runs
    }

    test("private chat: blank -> sends hint text, no job") {
        val sut = DownloadCommand(service, bot, NoopRateLimitGuard(), validator, queue)

        sut.handle(ctx(listOf("   "), 100, 42, "private"))

        bot.sentTexts.shouldHaveSize(1)
        val sent = bot.sentTexts.single()
        sent.chatId shouldBe 100
        sent.replyToMessageId shouldBe 42
        sent.text shouldBe "Будь ласка, вкажіть URL для завантаження."

        coVerify(exactly = 0) { queue.submit(any()) }
    }

    test("private chat: not an URL -> sends hint text, no job") {
        val sut = DownloadCommand(service, bot, NoopRateLimitGuard(), validator, queue)

        sut.handle(ctx(listOf("not-a-url"), 100, 42, "private"))

        bot.sentTexts.shouldHaveSize(1)
        coVerify(exactly = 0) { queue.submit(any()) }
    }

    test("private chat: valid URL -> submits job (trimmed), no hint") {
        val sut = DownloadCommand(service, bot, NoopRateLimitGuard(), validator, queue)

        sut.handle(ctx(listOf("  https://example.com/a  "), 101, 7, "private"))

        bot.sentTexts.shouldHaveSize(0)
        coVerify(exactly = 1) { queue.submit(any()) }

        val job = jobSlot.captured
        job.sourceUrl shouldBe "https://example.com/a"
        job.chatId shouldBe 101
        job.replyToMessageId shouldBe 7
    }

    test("group chat: valid URL but service does not support -> no side effects") {
        coEvery { service.supports(any()) } returns false
        val sut = DownloadCommand(service, bot, NoopRateLimitGuard(), validator, queue)

        sut.handle(ctx(listOf("https://example.com/x"), -10, 1, "group"))

        bot.sentTexts.shouldHaveSize(0)
        coVerify(exactly = 0) { queue.submit(any()) }
    }

    test("group chat: valid URL and service supports -> submits job") {
        coEvery { service.supports(any()) } returns true
        val sut = DownloadCommand(service, bot, NoopRateLimitGuard(), validator, queue)

        sut.handle(ctx(listOf("https://example.com/x"), -11, 99, "supergroup"))

        bot.sentTexts.shouldHaveSize(0)
        coVerify(exactly = 1) { queue.submit(any()) }
    }

    test("group chat: not an URL -> no side effects") {
        val sut = DownloadCommand(service, bot, NoopRateLimitGuard(), validator, queue)

        sut.handle(ctx(listOf("not-a-url"), -12, 5, "group"))

        bot.sentTexts.shouldHaveSize(0)
        coVerify(exactly = 0) { queue.submit(any()) }
    }

    test("rate limited: private invalid URL branch -> exception, no message sent") {
        val sut = DownloadCommand(service, bot, RejectAllRateLimitGuard(), validator, queue)

        shouldThrow<TooManyRequestsException> {
            sut.handle(ctx(listOf(" "), 100, 42, "private"))
        }

        bot.sentTexts.shouldHaveSize(0)
        coVerify(exactly = 0) { queue.submit(any()) }
    }

    test("rate limited: allowed branch -> exception, job not submitted") {
        val sut = DownloadCommand(service, bot, RejectAllRateLimitGuard(), validator, queue)

        shouldThrow<TooManyRequestsException> {
            sut.handle(ctx(listOf("https://example.com"), 100, 42, "private"))
        }

        bot.sentTexts.shouldHaveSize(0)
        coVerify(exactly = 0) { queue.submit(any()) }
    }
})

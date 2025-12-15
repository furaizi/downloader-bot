package com.download.downloaderbot.bot.ratelimit.guard

import com.download.downloaderbot.bot.commands.util.ctx
import com.download.downloaderbot.bot.ratelimit.limiter.RateLimiter
import com.download.downloaderbot.core.downloader.TooManyRequestsException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.*

class DefaultRateLimitGuardTest : FunSpec({

    lateinit var limiter: RateLimiter
    lateinit var guard: DefaultRateLimitGuard

    beforeTest {
        limiter = mockk()
        guard = DefaultRateLimitGuard(limiter)
    }

    test("rejects when local limit denies: does not await global and does not execute block") {
        val chatId = 52L
        val ctx = ctx(chatId = chatId)

        coEvery { limiter.tryConsumePerChatOrGroup(chatId) } returns false

        val ex = shouldThrow<TooManyRequestsException> {
            guard.runOrReject(ctx) { error("block must not be called when rate-limit rejects") }
        }

        ex.chatId shouldBe chatId
        ex.chatType shouldBe "private"

        coVerify(exactly = 1) { limiter.tryConsumePerChatOrGroup(chatId) }
        coVerify(exactly = 0) { limiter.awaitGlobal() }
    }

    test("rejects and identifies group chat when chatId is negative") {
        val chatId = -100500L
        val ctx = ctx(chatId = chatId)

        coEvery { limiter.tryConsumePerChatOrGroup(chatId) } returns false

        val ex = shouldThrow<TooManyRequestsException> {
            guard.runOrReject(ctx) { "nope" }
        }

        ex.chatId shouldBe chatId
        ex.chatType shouldBe "group"
        coVerify(exactly = 0) { limiter.awaitGlobal() }
    }

    test("passes when local ok: awaits global then executes block (order) and returns result") {
        val chatId = 777L
        val ctx = ctx(chatId = chatId)

        coEvery { limiter.tryConsumePerChatOrGroup(chatId) } returns true
        coJustRun { limiter.awaitGlobal() }

        var blockCalled = false

        val result = guard.runOrReject(ctx) {
            blockCalled = true
            "OK_RESULT"
        }

        result shouldBe "OK_RESULT"
        blockCalled shouldBe true

        coVerifySequence {
            limiter.tryConsumePerChatOrGroup(chatId)
            limiter.awaitGlobal()
        }
    }

    test("propagates exception from block after both limits passed") {
        val chatId = 1L
        val ctx = ctx(chatId = chatId)

        coEvery { limiter.tryConsumePerChatOrGroup(chatId) } returns true
        coJustRun { limiter.awaitGlobal() }

        val ex = shouldThrow<IllegalStateException> {
            guard.runOrReject(ctx) { error("boom") }
        }

        ex.message shouldBe "boom"

        coVerifySequence {
            limiter.tryConsumePerChatOrGroup(chatId)
            limiter.awaitGlobal()
        }
    }
})

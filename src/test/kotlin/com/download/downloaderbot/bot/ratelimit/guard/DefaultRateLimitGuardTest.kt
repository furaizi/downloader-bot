package com.download.downloaderbot.bot.ratelimit.guard

import com.download.downloaderbot.bot.commands.CommandContext
import com.download.downloaderbot.bot.ratelimit.limiter.RateLimiter
import com.download.downloaderbot.core.downloader.TooManyRequestsException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk

class DefaultRateLimitGuardTest : FunSpec({

    lateinit var limiter: RateLimiter
    lateinit var guard: DefaultRateLimitGuard
    lateinit var ctx: CommandContext

    beforeTest {
        limiter = mockk()
        guard = DefaultRateLimitGuard(limiter)

        ctx = mockk(relaxed = true)
    }

    test("rejects when local rate limit denies and does not call awaitGlobal or block") {
        coEvery { limiter.tryConsumePerChatOrGroup(any()) } returns false

        val ex = shouldThrow<TooManyRequestsException> {
            guard.runOrReject(ctx) {
                error("block must not be called when rate-limit rejects")
            }
        }

        coVerify(exactly = 1) { limiter.tryConsumePerChatOrGroup(any()) }
        coVerify(exactly = 0) { limiter.awaitGlobal() }
    }

    test("passes when local limit ok, awaits global and executes block") {
        coEvery { limiter.tryConsumePerChatOrGroup(any()) } returns true
        coEvery { limiter.awaitGlobal() } returns Unit

        var blockCalled = false

        val result = guard.runOrReject(ctx) {
            blockCalled = true
            "OK_RESULT"
        }

        result shouldBe "OK_RESULT"
        blockCalled shouldBe true

        coVerify(exactly = 1) { limiter.tryConsumePerChatOrGroup(any()) }
        coVerify(exactly = 1) { limiter.awaitGlobal() }
    }

    test("propagates exception from block after both rate limits passed") {
        coEvery { limiter.tryConsumePerChatOrGroup(any()) } returns true
        coEvery { limiter.awaitGlobal() } returns Unit

        val ex = shouldThrow<IllegalStateException> {
            guard.runOrReject(ctx) {
                error("boom")
            }
        }

        ex.message shouldBe "boom"

        coVerify(exactly = 1) { limiter.tryConsumePerChatOrGroup(any()) }
        coVerify(exactly = 1) { limiter.awaitGlobal() }
    }
})

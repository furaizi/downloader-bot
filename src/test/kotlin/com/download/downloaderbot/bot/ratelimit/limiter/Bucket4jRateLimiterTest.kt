package com.download.downloaderbot.bot.ratelimit.limiter

import com.download.downloaderbot.bot.config.properties.RateLimitProperties
import com.download.downloaderbot.bot.config.properties.fingerprint
import com.fasterxml.jackson.databind.ObjectMapper
import io.github.bucket4j.BucketConfiguration
import io.github.bucket4j.ConsumptionProbe
import io.github.bucket4j.distributed.AsyncBucketProxy
import io.github.bucket4j.distributed.proxy.AsyncProxyManager
import io.github.bucket4j.distributed.proxy.ProxyManager
import io.github.bucket4j.distributed.proxy.RemoteAsyncBucketBuilder
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.concurrent.CompletableFuture
import java.util.function.Supplier
import kotlin.coroutines.cancellation.CancellationException

typealias BucketConfigSupplier = Supplier<CompletableFuture<BucketConfiguration>>

class Bucket4jRateLimiterTest : FunSpec({

    lateinit var proxyManager: ProxyManager<String>
    lateinit var asyncProxyManager: AsyncProxyManager<String>
    lateinit var builder: RemoteAsyncBucketBuilder<String>
    lateinit var bucket: AsyncBucketProxy
    val mapper =
        ObjectMapper()
            .findAndRegisterModules()

    beforeTest {
        proxyManager = mockk()
        asyncProxyManager = mockk()
        builder = mockk()
        bucket = mockk()

        every { proxyManager.asAsync() } returns asyncProxyManager
        every { asyncProxyManager.builder() } returns builder
        every {
            builder.build(any(), any<BucketConfigSupplier>())
        } returns bucket
    }

    test("awaitGlobal returns immediately when rate limit disabled") {
        val props = RateLimitProperties(enabled = false)

        val limiter = Bucket4jRateLimiter(proxyManager, props, mapper)

        limiter.awaitGlobal()

        verify(exactly = 0) { asyncProxyManager.builder() }
        verify(exactly = 0) { bucket.tryConsumeAndReturnRemaining(any()) }
    }

    test("awaitGlobal consumes once when probe is immediately allowed") {
        val props = RateLimitProperties(enabled = true, namespace = "rl-test")
        val version = props.fingerprint(mapper)
        val limiter = Bucket4jRateLimiter(proxyManager, props, mapper)

        val probe = mockk<ConsumptionProbe>()
        every { probe.isConsumed } returns true
        every { probe.remainingTokens } returns 9L
        every { probe.nanosToWaitForRefill } returns 0L
        every { bucket.tryConsumeAndReturnRemaining(1) } returns
            CompletableFuture.completedFuture(probe)

        limiter.awaitGlobal()

        val expectedKey = "${props.namespace}:v$version:global"

        verify(exactly = 1) { builder.build(expectedKey, any<BucketConfigSupplier>()) }
        verify(exactly = 1) { bucket.tryConsumeAndReturnRemaining(1) }
    }

    test("awaitGlobal waits and retries until tokens are available") {
        val props = RateLimitProperties(enabled = true)
        val limiter = Bucket4jRateLimiter(proxyManager, props, mapper)

        val denied = mockk<ConsumptionProbe>()
        every { denied.isConsumed } returns false
        every { denied.remainingTokens } returns 0L
        every { denied.nanosToWaitForRefill } returns 0L

        val allowed = mockk<ConsumptionProbe>()
        every { allowed.isConsumed } returns true
        every { allowed.remainingTokens } returns 10L
        every { allowed.nanosToWaitForRefill } returns 0L

        every { bucket.tryConsumeAndReturnRemaining(1) } returnsMany
            listOf(
                CompletableFuture.completedFuture(denied),
                CompletableFuture.completedFuture(allowed),
            )

        limiter.awaitGlobal()

        verify(exactly = 2) { bucket.tryConsumeAndReturnRemaining(1) }
    }

    test("tryConsumePerChatOrGroup returns true and does not touch bucket when disabled") {
        val props = RateLimitProperties(enabled = false)
        val limiter = Bucket4jRateLimiter(proxyManager, props, mapper)

        val result = limiter.tryConsumePerChatOrGroup(123L)

        result.shouldBeTrue()
        verify(exactly = 0) { asyncProxyManager.builder() }
        verify(exactly = 0) { bucket.tryConsume(any()) }
    }

    test("tryConsumePerChatOrGroup uses chat bucket for positive chatId and returns backend decision") {
        val props = RateLimitProperties(enabled = true, namespace = "rl-chat")
        val version = props.fingerprint(mapper)
        val limiter = Bucket4jRateLimiter(proxyManager, props, mapper)

        every { bucket.tryConsume(1) } returns CompletableFuture.completedFuture(true)

        val result = limiter.tryConsumePerChatOrGroup(42L)

        result.shouldBeTrue()

        val expectedKey = "${props.namespace}:v$version:chat:42"
        verify(exactly = 1) { builder.build(expectedKey, any<BucketConfigSupplier>()) }
        verify(exactly = 1) { bucket.tryConsume(1) }
    }

    test("tryConsumePerChatOrGroup uses group bucket for negative chatId and returns false when limit exceeded") {
        val props = RateLimitProperties(enabled = true, namespace = "rl-group")
        val version = props.fingerprint(mapper)
        val limiter = Bucket4jRateLimiter(proxyManager, props, mapper)

        every { bucket.tryConsume(1) } returns CompletableFuture.completedFuture(false)

        val result = limiter.tryConsumePerChatOrGroup(-100L)

        result.shouldBeFalse()

        val expectedKey = "${props.namespace}:v$version:group:-100"
        verify(exactly = 1) { builder.build(expectedKey, any<BucketConfigSupplier>()) }
        verify(exactly = 1) { bucket.tryConsume(1) }
    }

    test("tryConsumePerChatOrGroup fails open on non-cancellation exception") {
        val props = RateLimitProperties(enabled = true)
        val limiter = Bucket4jRateLimiter(proxyManager, props, mapper)

        val future =
            CompletableFuture<Boolean>().apply {
                completeExceptionally(RuntimeException("Redis down"))
            }
        every { bucket.tryConsume(1) } returns future

        val result = limiter.tryConsumePerChatOrGroup(123L)

        result.shouldBeTrue()
        verify(exactly = 1) { bucket.tryConsume(1) }
    }

    test("tryConsumePerChatOrGroup rethrows CancellationException") {
        val props = RateLimitProperties(enabled = true)
        val limiter = Bucket4jRateLimiter(proxyManager, props, mapper)

        val future =
            CompletableFuture<Boolean>().apply {
                completeExceptionally(CancellationException("cancel"))
            }
        every { bucket.tryConsume(1) } returns future

        shouldThrow<CancellationException> {
            limiter.tryConsumePerChatOrGroup(123L)
        }

        verify(exactly = 1) { bucket.tryConsume(1) }
    }
})

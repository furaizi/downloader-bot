package com.download.downloaderbot.bot.ratelimit.limiter

import com.download.downloaderbot.bot.config.properties.RateLimitProperties
import com.download.downloaderbot.bot.config.properties.fingerprint
import com.fasterxml.jackson.databind.ObjectMapper
import io.github.bucket4j.Bandwidth
import io.github.bucket4j.BandwidthBuilder
import io.github.bucket4j.BucketConfiguration
import io.github.bucket4j.distributed.AsyncBucketProxy
import io.github.bucket4j.distributed.proxy.AsyncProxyManager
import io.github.bucket4j.distributed.proxy.ProxyManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.future.await
import mu.KotlinLogging
import java.util.concurrent.CompletableFuture
import kotlin.math.max

private val log = KotlinLogging.logger {}
private const val MIN_DELAY_MS = 1L
private const val NANOS_PER_MILLISECOND = 1_000_000L

class Bucket4jRateLimiter(
    proxyManager: ProxyManager<String>,
    private val props: RateLimitProperties,
    private val mapper: ObjectMapper,
) : RateLimiter {
    private val async: AsyncProxyManager<String> = proxyManager.asAsync()
    private val version: String by lazy { props.fingerprint(mapper) }

    init {
        log.info { "[rate-limit] config version=$version" }
    }

    override suspend fun awaitGlobal() {
        if (!props.enabled) return
        val key = keyGlobal()
        val bucket = asyncBucket(key, props.global)
        while (true) {
            val probe = bucket.tryConsumeAndReturnRemaining(1).await()
            if (probe.isConsumed) {
                log.debug { "[rate-limit] global OK key=$key remaining=${probe.remainingTokens}" }
                return
            }
            val ms = max(MIN_DELAY_MS, probe.nanosToWaitForRefill / NANOS_PER_MILLISECOND)
            log.info { "[rate-limit] global THROTTLE key=$key wait=${ms}ms remaining=${probe.remainingTokens}" }
            delay(ms)
        }
    }

    override suspend fun tryConsumePerChatOrGroup(chatId: Long): Boolean {
        if (!props.enabled) return true
        val (key, cfg) =
            if (isGroup(chatId)) {
                keyGroup(chatId) to props.group
            } else {
                keyChat(chatId) to props.chat
            }
        val bucket = asyncBucket(key, cfg)
        return try {
            val ok = bucket.tryConsume(1).await()
            if (ok) {
                log.debug { "[rate-limit] local OK key=$key" }
            } else {
                log.info { "[rate-limit] local REJECT key=$key" }
            }
            ok
        } catch (e: Exception) {
            log.warn(e) { "[rate-limit] local FAIL-OPEN key=$key (Redis error)" }
            true
        }
    }

    private fun asyncBucket(
        key: String,
        cfg: RateLimitProperties.Bucket,
    ): AsyncBucketProxy {
        log.debug(
            "[rate-limit] build bucket key={} cap={} refill={}/{} greedy={}",
            key,
            cfg.capacity,
            cfg.refill.tokens,
            cfg.refill.period,
            cfg.refill.greedy,
        )
        val config =
            BucketConfiguration.builder()
                .addLimit(cfg.toBandwidth())
                .build()
        return async.builder().build(key) { CompletableFuture.completedFuture(config) }
    }

    private fun isGroup(chatId: Long): Boolean = chatId < 0

    private fun keyGlobal(): String = "${props.namespace}:v$version:global"

    private fun keyChat(chatId: Long): String = "${props.namespace}:v$version:chat:$chatId"

    private fun keyGroup(chatId: Long): String = "${props.namespace}:v$version:group:$chatId"

    private fun RateLimitProperties.Bucket.toBandwidth(): Bandwidth {
        val builder =
            BandwidthBuilder.builder()
                .capacity(capacity)
        return if (refill.greedy) {
            builder
                .refillGreedy(refill.tokens, refill.period)
                .build()
        } else {
            builder
                .refillIntervally(refill.tokens, refill.period)
                .build()
        }
    }
}

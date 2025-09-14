package com.download.downloaderbot.bot.ratelimit

import com.download.downloaderbot.bot.config.properties.RateLimitProperties
import io.github.bucket4j.Bandwidth
import io.github.bucket4j.BandwidthBuilder
import io.github.bucket4j.BucketConfiguration
import io.github.bucket4j.distributed.proxy.ProxyManager
import kotlinx.coroutines.future.await
import org.springframework.stereotype.Component
import java.util.concurrent.CompletableFuture

@Component
class Bucket4jRateLimiter(
    private val proxyManager: ProxyManager<String>,
    private val props: RateLimitProperties
) : RateLimiter {

    override suspend fun allow(chatId: Long): Boolean {
        if (!props.enabled) return true
        if (!consume(keyGlobal(), props.global))
            return false

        return  if (isGroup(chatId))
                    consume(keyGroup(chatId), props.group)
                else
                    consume(keyChat(chatId), props.chat)

    }

    private fun isGroup(chatId: Long): Boolean = chatId < 0

    private fun keyGlobal(): String = "${props.namespace}:global"
    private fun keyChat(chatId: Long): String = "${props.namespace}:chat:$chatId"
    private fun keyGroup(chatId: Long): String = "${props.namespace}:group:$chatId"

    private suspend fun consume(key: String, cfg: RateLimitProperties.Bucket): Boolean =
        try {
            val config = BucketConfiguration.builder()
                .addLimit(cfg.toBandwidth())
                .build()

            val asyncBucket = proxyManager.asAsync()
                .builder()
                .build(key) { CompletableFuture.completedFuture(config) }

            asyncBucket
                .tryConsume(1)
                .await()
        } catch (_: Exception) {
            true
        }

    private fun RateLimitProperties.Bucket.toBandwidth(): Bandwidth {
        val builder = BandwidthBuilder.builder()
            .capacity(capacity)
        return  if (refill.greedy)
                    builder
                        .refillGreedy(refill.tokens, refill.period)
                        .build()
                else builder
                        .refillIntervally(refill.tokens, refill.period)
                        .build()
    }

}
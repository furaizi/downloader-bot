package com.download.downloaderbot.bot.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "downloader.ratelimit")
data class RateLimitProperties(
    val enabled: Boolean = true,
    val namespace: String = "rl",
    val global: Bucket = Bucket(),
    val chat: Bucket = Bucket(capacity = 1, refill = Refill(tokens = 1, period = Duration.ofSeconds(1), greedy = true)),
    val group: Bucket = Bucket(capacity = 20, refill = Refill(tokens = 20, period = Duration.ofMinutes(1), greedy = true)),
) {
    data class Bucket(
        val capacity: Long = 30,
        val refill: Refill = Refill(tokens = 30, period = Duration.ofSeconds(1), greedy = true)
    )
    data class Refill(
        val tokens: Long,
        val period: Duration,
        val greedy: Boolean = true
    )
}
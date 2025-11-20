package com.download.downloaderbot.bot.config.properties

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.boot.context.properties.ConfigurationProperties
import java.security.MessageDigest
import java.time.Duration

private const val FINGERPRINT_LENGTH = 8

@ConfigurationProperties(prefix = "downloader.ratelimit")
data class RateLimitProperties(
    val enabled: Boolean = true,
    val namespace: String = "rl",
    val global: Bucket = Bucket(),
    val chat: Bucket =
        Bucket(
            capacity = 1,
            refill =
                Refill(
                    tokens = 1,
                    period = Duration.ofSeconds(1),
                    greedy = true,
                ),
        ),
    val group: Bucket =
        Bucket(
            capacity = 20,
            refill =
                Refill(
                    tokens = 20,
                    period = Duration.ofMinutes(1),
                    greedy = true,
                ),
        ),
) {
    data class Bucket(
        val capacity: Long = 30,
        val refill: Refill = Refill(tokens = 30, period = Duration.ofSeconds(1), greedy = true),
    )

    data class Refill(
        val tokens: Long,
        val period: Duration,
        val greedy: Boolean = true,
    )
}

fun RateLimitProperties.fingerprint(mapper: ObjectMapper): String {
    val norm =
        copy(
            global = global.normalizeRefillPeriod(),
            chat = chat.normalizeRefillPeriod(),
            group = group.normalizeRefillPeriod(),
        )
    val bytes = mapper.writeValueAsBytes(norm)
    val hash = MessageDigest.getInstance("SHA-256").digest(bytes)
    return hash.joinToString("") { "%02x".format(it) }
        .take(FINGERPRINT_LENGTH)
}

private fun RateLimitProperties.Bucket.normalizeRefillPeriod(): RateLimitProperties.Bucket =
    copy(refill = refill.copy(period = Duration.ofMillis(refill.period.toMillis())))

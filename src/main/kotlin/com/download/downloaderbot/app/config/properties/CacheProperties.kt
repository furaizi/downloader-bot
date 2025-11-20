package com.download.downloaderbot.app.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

private const val DEFAULT_MEDIA_TTL_DAYS = 7L
private const val DEFAULT_LOCK_TTL_SECONDS = 60L
private const val DEFAULT_WAIT_TIMEOUT_SECONDS = 60L
private const val DEFAULT_WAIT_POLL_SECONDS = 1L

@ConfigurationProperties(prefix = "downloader.cache")
data class CacheProperties(
    val schemaVersion: Int,
    val mediaTtl: Duration = Duration.ofDays(DEFAULT_MEDIA_TTL_DAYS),
    val lockTtl: Duration = Duration.ofSeconds(DEFAULT_LOCK_TTL_SECONDS),
    val waitTimeout: Duration = Duration.ofSeconds(DEFAULT_WAIT_TIMEOUT_SECONDS),
    val waitPoll: Duration = Duration.ofSeconds(DEFAULT_WAIT_POLL_SECONDS),
)

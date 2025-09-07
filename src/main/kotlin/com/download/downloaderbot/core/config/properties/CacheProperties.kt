package com.download.downloaderbot.core.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "downloader.cache")
data class CacheProperties(
    val mediaTtl: Duration = Duration.ofDays(7),
    val lockTtl: Duration = Duration.ofSeconds(60),
    val waitTimeout: Duration = Duration.ofSeconds(60),
    val waitPoll: Duration = Duration.ofSeconds(1)
)
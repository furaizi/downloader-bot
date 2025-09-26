package com.download.downloaderbot.app.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "downloader.instaloader")
data class InstaloaderProperties(
    val bin: String,
    val sessionFile: String? = null,
    val userAgent: String? = null,
    val extraArgs: List<String> = emptyList(),
    val timeout: Duration = Duration.ofMinutes(2),
)

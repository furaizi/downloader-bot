package com.download.downloaderbot.app.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "downloader.yt-dlp")
data class YtDlpProperties(
    val bin: String,
    val configFile: String = "",
    val timeout: Duration = Duration.ofMinutes(2),
)

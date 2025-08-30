package com.download.downloaderbot.core.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "downloader.yt-dlp")
data class YtDlpProperties(
    val bin: String,
    val format: String = "",
    val extraArgs: List<String> = emptyList()
)
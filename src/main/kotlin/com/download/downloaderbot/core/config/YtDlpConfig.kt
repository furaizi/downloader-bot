package com.download.downloaderbot.core.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "downloader.ytdlp")
data class YtDlpConfig(
    val bin: String,
    val format: String,
    val extraArgs: List<String> = emptyList()
)
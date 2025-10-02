package com.download.downloaderbot.app.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "downloader.sources")
data class SourceAllowProperties(
    val allow: List<String> = emptyList(),
)

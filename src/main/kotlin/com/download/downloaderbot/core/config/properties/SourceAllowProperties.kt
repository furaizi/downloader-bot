package com.download.downloaderbot.core.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "downloader.sources")
data class SourceAllowProperties(
    val allow: List<String> = emptyList()
)

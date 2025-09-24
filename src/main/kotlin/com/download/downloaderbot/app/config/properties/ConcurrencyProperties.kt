package com.download.downloaderbot.app.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "downloader.concurrency")
data class ConcurrencyProperties(
    val maxDownloads: Int = 3
)
package com.download.downloaderbot.core.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "downloader.cache")
data class CacheProperties(
    val ttlDays: Long = 7
)
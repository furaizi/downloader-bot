package com.download.downloaderbot.core.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "downloader.gallery-dl")
data class GalleryDlProperties(
    val bin: String,
    val baseDir: String,
    val extraArgs: List<String> = emptyList()
)

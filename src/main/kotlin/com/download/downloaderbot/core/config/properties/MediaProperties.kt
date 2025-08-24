package com.download.downloaderbot.core.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import java.nio.file.Paths

@ConfigurationProperties(prefix = "downloader.media")
data class MediaProperties(
    val baseDir: String
) {
    val basePath = Paths.get(baseDir)
}
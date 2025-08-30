package com.download.downloaderbot.core.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.util.unit.DataSize
import java.nio.file.Paths

@ConfigurationProperties(prefix = "downloader.media")
data class MediaProperties(
    val baseDir: String,
    val maxSize: MediaSizeLimits
) {
    val basePath = Paths.get(baseDir)
}

data class MediaSizeLimits(
    val photo: DataSize = DataSize.ofMegabytes(10),
    val video: DataSize = DataSize.ofMegabytes(50)
)
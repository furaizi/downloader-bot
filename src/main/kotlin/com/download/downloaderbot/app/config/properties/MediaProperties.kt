package com.download.downloaderbot.app.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.util.unit.DataSize
import java.nio.file.Paths
import java.time.Duration

@ConfigurationProperties(prefix = "downloader.media")
data class MediaProperties(
    val baseDir: String,
    val maxSize: MediaSizeLimits = MediaSizeLimits(),
    val cleanup: MediaCleanupProperties = MediaCleanupProperties()
) {
    val basePath = Paths.get(baseDir)
}

data class MediaSizeLimits(
    val photo: DataSize = DataSize.ofMegabytes(10),
    val video: DataSize = DataSize.ofMegabytes(50)
)

data class MediaCleanupProperties(
    val maxAge: Duration = Duration.ofDays(7),
    val maxTotalSize: DataSize = DataSize.ofGigabytes(10),
    val interval: Duration = Duration.ofHours(1),
    val initialDelay: Duration = Duration.ofMinutes(5)
)

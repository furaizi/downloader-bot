package com.download.downloaderbot.app.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.util.unit.DataSize
import java.nio.file.Paths
import java.time.Duration

private const val DEFAULT_PHOTO_MAX_SIZE_MB = 10L
private const val DEFAULT_VIDEO_MAX_SIZE_MB = 50L
private const val DEFAULT_CLEANUP_MAX_AGE_DAYS = 7L
private const val DEFAULT_MAX_TOTAL_SIZE_GB = 10L
private const val DEFAULT_CLEANUP_INTERVAL_HOURS = 1L
private const val DEFAULT_CLEANUP_INITIAL_DELAY_MINUTES = 5L

@ConfigurationProperties(prefix = "downloader.media")
data class MediaProperties(
    val baseDir: String,
    val maxSize: MediaSizeLimits = MediaSizeLimits(),
    val cleanup: MediaCleanupProperties = MediaCleanupProperties(),
) {
    val basePath = Paths.get(baseDir)
}

data class MediaSizeLimits(
    val photo: DataSize = DataSize.ofMegabytes(DEFAULT_PHOTO_MAX_SIZE_MB),
    val video: DataSize = DataSize.ofMegabytes(DEFAULT_VIDEO_MAX_SIZE_MB),
)

data class MediaCleanupProperties(
    val maxAge: Duration = Duration.ofDays(DEFAULT_CLEANUP_MAX_AGE_DAYS),
    val maxTotalSize: DataSize = DataSize.ofGigabytes(DEFAULT_MAX_TOTAL_SIZE_GB),
    val interval: Duration = Duration.ofHours(DEFAULT_CLEANUP_INTERVAL_HOURS),
    val initialDelay: Duration = Duration.ofMinutes(DEFAULT_CLEANUP_INITIAL_DELAY_MINUTES),
)

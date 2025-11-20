package com.download.downloaderbot.infra.cleanup

import com.download.downloaderbot.app.config.properties.MediaProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import org.springframework.stereotype.Service
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import java.time.Instant
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile
import kotlin.streams.asSequence

private val log = KotlinLogging.logger {}

@Service
class MediaCleanupService(
    private val mediaProperties: MediaProperties,
) {
    suspend fun cleanup(): MediaCleanupReport {
        val cleanupProps = mediaProperties.cleanup
        val threshold = computeThreshold(cleanupProps.maxAge)

        val remaining = loadFiles(mediaProperties.basePath).toMutableList()

        val expired = removeExpired(remaining, threshold)
        val trimmed = trimToMaxSize(remaining, cleanupProps.maxTotalSize.toBytes())

        return MediaCleanupReport(
            deletedFiles = expired.files + trimmed.files,
            freedBytes = expired.bytes + trimmed.bytes,
        )
    }

    private suspend fun loadFiles(basePath: Path): List<MediaFile> =
        withContext(Dispatchers.IO) {
            if (!basePath.exists()) return@withContext emptyList()
            try {
                Files.walk(basePath).use { stream ->
                    stream.asSequence()
                        .filter { it.isRegularFile() }
                        .mapNotNull { toMediaFile(it) }
                        .toList()
                }
            } catch (ex: IOException) {
                log.warn(ex) { "Failed to enumerate media files in $basePath" }
                emptyList()
            }
        }

    private fun computeThreshold(maxAge: Duration): Instant? {
        require(!maxAge.isNegative) { "downloader.media.cleanup.max-age must not be negative: $maxAge" }
        return if (maxAge.isZero) null else Instant.now().minus(maxAge)
    }

    private suspend fun removeExpired(
        remaining: MutableList<MediaFile>,
        threshold: Instant?,
    ): CleanupDelta {
        if (threshold == null) return CleanupDelta.ZERO

        var deletedFiles = 0
        var freedBytes = 0L
        val expired = remaining.filter { it.lastModified.isBefore(threshold) }
        for (file in expired) {
            if (deleteFile(file)) {
                deletedFiles += 1
                freedBytes += file.size
                remaining.remove(file)
            }
        }
        return CleanupDelta(deletedFiles, freedBytes)
    }

    private suspend fun trimToMaxSize(
        remaining: MutableList<MediaFile>,
        maxBytes: Long,
    ): CleanupDelta {
        var totalSize = remaining.sumOf { it.size }
        if (maxBytes <= 0 || totalSize <= maxBytes) return CleanupDelta.ZERO

        var deletedFiles = 0
        var freedBytes = 0L
        val orderedByAge = remaining.sortedBy { it.lastModified }
        for (file in orderedByAge) {
            if (totalSize <= maxBytes) break
            if (deleteFile(file)) {
                deletedFiles += 1
                freedBytes += file.size
                totalSize -= file.size
                remaining.remove(file)
            }
        }
        return CleanupDelta(deletedFiles, freedBytes)
    }

    @SuppressWarnings("TooGenericExceptionCaught")
    private fun toMediaFile(path: Path): MediaFile? =
        runCatching {
            val size = Files.size(path)
            val lastModified = Files.getLastModifiedTime(path).toInstant()
            MediaFile(path, size, lastModified)
        }.getOrElse { ex ->
            log.warn(ex) { "Unable to read metadata of $path" }
            null
        }

    private suspend fun deleteFile(file: MediaFile): Boolean =
        withContext(Dispatchers.IO) {
            try {
                Files.deleteIfExists(file.path).also { deleted ->
                    if (deleted) {
                        log.info { "Deleted media file ${file.path} (freed ${file.size} bytes)" }
                    }
                }
            } catch (ex: IOException) {
                log.warn(ex) { "Failed to delete media file ${file.path}" }
                false
            }
        }

    private data class MediaFile(
        val path: Path,
        val size: Long,
        val lastModified: Instant,
    )

    private data class CleanupDelta(
        val files: Int,
        val bytes: Long,
    ) {
        companion object {
            val ZERO = CleanupDelta(0, 0L)
        }
    }
}

data class MediaCleanupReport(
    val deletedFiles: Int = 0,
    val freedBytes: Long = 0L,
)

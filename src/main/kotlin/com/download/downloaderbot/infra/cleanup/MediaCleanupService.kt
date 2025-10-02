package com.download.downloaderbot.infra.cleanup

import com.download.downloaderbot.app.config.properties.MediaProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import org.springframework.stereotype.Service
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
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
        val basePath = mediaProperties.basePath

        val maxAge = cleanupProps.maxAge
        require(!maxAge.isNegative) { "downloader.media.cleanup.max-age must not be negative: $maxAge" }

        val threshold: Instant? =
            if (maxAge.isZero) {
                null
            } else {
                Instant.now().minus(maxAge)
            }

        var deletedFiles = 0
        var freedBytes = 0L

        val remaining = loadFiles(basePath).toMutableList()

        if (threshold != null) {
            val expired = remaining.filter { it.lastModified.isBefore(threshold) }
            for (file in expired) {
                if (deleteFile(file)) {
                    deletedFiles += 1
                    freedBytes += file.size
                    remaining.remove(file)
                }
            }
        }

        val maxBytes = cleanupProps.maxTotalSize.toBytes()
        if (maxBytes > 0) {
            var totalSize = remaining.sumOf { it.size }
            if (totalSize > maxBytes) {
                val orderedByAge = remaining.sortedBy { it.lastModified }
                for (file in orderedByAge) {
                    if (totalSize <= maxBytes) break
                    if (deleteFile(file)) {
                        deletedFiles += 1
                        freedBytes += file.size
                        totalSize -= file.size
                    }
                }
            }
        }

        return MediaCleanupReport(deletedFiles, freedBytes)
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
            } catch (ex: Exception) {
                log.warn(ex) { "Failed to enumerate media files in $basePath" }
                emptyList()
            }
        }

    private fun toMediaFile(path: Path): MediaFile? {
        val size =
            try {
                Files.size(path)
            } catch (t: Exception) {
                log.warn(t) { "Unable to read size of $path" }
                return null
            }
        val lastModified =
            try {
                Files.getLastModifiedTime(path).toInstant()
            } catch (t: Exception) {
                log.warn(t) { "Unable to read lastModified of $path" }
                return null
            }
        return MediaFile(path, size, lastModified)
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
}

data class MediaCleanupReport(
    val deletedFiles: Int = 0,
    val freedBytes: Long = 0L,
)

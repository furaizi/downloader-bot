package com.download.downloaderbot.core.storage

import com.download.downloaderbot.core.cache.media.MediaCache
import com.download.downloaderbot.core.config.properties.MediaProperties
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.springframework.stereotype.Service
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.streams.asSequence

private val log = KotlinLogging.logger {}

@Service
class MediaCleanupService(
    private val mediaProperties: MediaProperties,
    private val mediaCache: MediaCache
) {

    fun cleanup(): MediaCleanupReport {
        val cleanupProps = mediaProperties.cleanup
        val basePath = mediaProperties.basePath

        val now = Instant.now()
        val maxAge = cleanupProps.maxAge
        require(!maxAge.isNegative) { "downloader.media.cleanup.max-age must not be negative: $maxAge" }

        val threshold = if (maxAge.isZero)
            null
        else
            now.minus(maxAge)

        var deletedFiles = 0
        var freedBytes = 0L

        if (threshold != null) {
            val candidates = loadFiles(basePath)
                .filter { it.lastModified.isBefore(threshold) }

            for (file in candidates) {
                if (deleteFile(file)) {
                    deletedFiles += 1
                    freedBytes += file.size
                    evictCacheFor(file.path)
                }
            }
        }

        val filesAfterExpiration = loadFiles(basePath)

        val maxBytes = cleanupProps.maxTotalSize.toBytes()
        if (maxBytes > 0) {
            var totalSize = filesAfterExpiration.sumOf { it.size }
            if (totalSize > maxBytes) {
                val orderedByAge = filesAfterExpiration.sortedBy { it.lastModified }
                for (file in orderedByAge) {
                    if (totalSize <= maxBytes) {
                        break
                    }
                    if (deleteFile(file)) {
                        deletedFiles += 1
                        freedBytes += file.size
                        totalSize -= file.size
                        evictCacheFor(file.path)
                    }
                }
            }
        }

        return MediaCleanupReport(deletedFiles, freedBytes)
    }

    private fun evictCacheFor(path: Path) = runBlocking {
        mediaCache.evictByPath(path)
        log.debug { "Evicted cache entries by path=$path" }
    }

    private fun loadFiles(basePath: Path): List<MediaFile> {
        if (!basePath.exists()) return emptyList()
        return runCatching {
            Files.walk(basePath)
                .use { stream ->
                    stream.asSequence()
                        .filter { it.isRegularFile() }
                        .mapNotNull { path ->
                            val size = runCatching { Files.size(path) }
                                .onFailure { log.warn(it) { "Unable to read size of $path" } }
                                .getOrNull()
                            val lastModified = runCatching { Files.getLastModifiedTime(path).toInstant() }
                                .onFailure { log.warn(it) { "Unable to read lastModified of $path" } }
                                .getOrNull()
                            if (size == null || lastModified == null) {
                                null
                            } else {
                                MediaFile(path, size, lastModified)
                            }
                        }
                        .toList()
                }
        }.getOrElse { ex ->
            log.warn(ex) { "Failed to enumerate media files in $basePath" }
            emptyList()
        }
    }

    private fun deleteFile(file: MediaFile): Boolean {
        return try {
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
        val lastModified: Instant
    )
}

data class MediaCleanupReport(
    val deletedFiles: Int = 0,
    val freedBytes: Long = 0L
)

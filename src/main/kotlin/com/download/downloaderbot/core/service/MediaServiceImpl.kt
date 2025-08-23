package com.download.downloaderbot.core.service

import com.download.downloaderbot.core.downloader.MediaDownloader
import com.download.downloaderbot.core.entity.Media
import com.download.downloaderbot.core.mediainfo.MediaInfoExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import org.springframework.stereotype.Service
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.attribute.FileTime
import java.time.Instant
import java.util.UUID
import kotlin.io.path.isRegularFile
import kotlin.io.path.name
import kotlin.streams.asSequence

private val log = KotlinLogging.logger {}

@Service
class MediaServiceImpl(
    val downloader: MediaDownloader,
    val mediaInfoExtractor: MediaInfoExtractor,
    private val downloadsRoot: Path = Paths.get(System.getProperty("user.home"), "Downloads", "downloader-bot")
) : MediaService {

    override suspend fun download(url: String): Media {
        log.info { "MediaService: requested download for url=$url" }
        prepareDownloadsDirectory()
        val basePrefix = generateBasePrefix(url)
        val outputTemplate = generateFilePathTemplate(basePrefix)
        log.info { "Starting downloader for url=$url to path=$outputTemplate" }

        safeDownload(url, outputTemplate)
        val downloadedFile = findDownloadedFileOrThrow(basePrefix)
        log.info { "Download completed. File located: ${downloadedFile.toAbsolutePath()}" }

        val media = safeFetchMediaInfo(url, downloadedFile.toAbsolutePath().toString())
        log.info { "Media created: ${media.title} (${media.fileUrl})" }
        return media
    }

    private suspend fun prepareDownloadsDirectory(): Unit = withContext(Dispatchers.IO) {
        try {
            if (Files.notExists(downloadsRoot)) {
                Files.createDirectories(downloadsRoot)
                log.debug { "Created downloads directory: $downloadsRoot" }
            }
        } catch (e: Exception) {
            log.error(e) { "Failed to create downloads directory: $downloadsRoot" }
            throw RuntimeException("Cannot create downloads directory", e)
        }
    }

    private fun generateBasePrefix(url: String): String {
        val host = runCatching { URI(url).host?.replace(":", "-") }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }
            ?: "media"
        val timestamp = Instant.now().toEpochMilli()
        val shortUuid = UUID.randomUUID().toString().take(8)
        return "$host-$timestamp-$shortUuid"
    }

    private fun generateFilePathTemplate(basePrefix: String) = downloadsRoot.resolve("$basePrefix.%(ext)s").toString()

    private suspend fun safeDownload(url: String, outputPath: String) = try {
            downloader.download(url, outputPath)
        } catch (e: Exception) {
            log.error(e) { "Download failed for url=$url" }
            throw RuntimeException("Download failed for url=$url", e)
        }

    private suspend fun findDownloadedFileOrThrow(basePrefix: String) =
        findLatestMatchingFile(downloadsRoot, basePrefix) ?: run {
            val msg = "Downloaded file not found for prefix $basePrefix in $downloadsRoot"
            log.error { msg }
            throw RuntimeException(msg)
        }

    private suspend fun safeFetchMediaInfo(url: String, path: String): Media = try {
        mediaInfoExtractor.fetchMediaInfo(url, path)
    } catch (e: Exception) {
        log.error(e) { "Failed to extract media info for file=${path}" }
        throw RuntimeException("Failed to extract media info", e)
    }

    private suspend fun findLatestMatchingFile(dir: Path, prefix: String): Path? = withContext(Dispatchers.IO) {
            Files.list(dir).use { stream ->
                stream.asSequence()
                    .filter { it.isRegularFile() && it.name.startsWith(prefix) }
                    .map { it to runCatching { Files.getLastModifiedTime(it) }.getOrDefault(FileTime.fromMillis(0)) }
                    .maxByOrNull { it.second.toMillis() }
                    ?.first
            }
        }
}
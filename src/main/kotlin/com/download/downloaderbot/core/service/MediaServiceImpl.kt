package com.download.downloaderbot.core.service

import com.download.downloaderbot.core.downloader.MediaDownloader
import com.download.downloaderbot.core.entity.Media
import com.download.downloaderbot.core.mediainfo.MediaInfoExtractor
import mu.KotlinLogging
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Instant
import java.util.UUID
import kotlin.io.path.isRegularFile
import kotlin.io.path.name
import kotlin.streams.asSequence

private val log = KotlinLogging.logger {}

class MediaServiceImpl(
    val downloader: MediaDownloader,
    val mediaInfoExtractor: MediaInfoExtractor,
    private val downloadsRoot: Path = Paths.get(System.getProperty("user.home"), "Downloads", "downloader-bot")
) : MediaService {

    override suspend fun download(url: String): Media {
        log.info { "MediaService: requested download for url=$url" }
        prepareDownloadsDirectory()
        val basePrefix = generateBasePrefix(url)
        val outputTemplate = generateFileTemplate(basePrefix)
        log.info { "Starting downloader for url=$url to path=$outputTemplate" }

        try {
            downloader.download(url, outputTemplate)
        } catch (e: Exception) {
            log.error(e) { "Download failed for url=$url" }
            throw RuntimeException("Download failed for url=$url", e)
        }

        val downloadedFile = findFirstMatchingFile(downloadsRoot, basePrefix) ?: run {
            val msg = "Downloaded file not found for prefix $basePrefix in $downloadsRoot"
            log.error { msg }
            throw RuntimeException(msg)
        }

        log.info { "Download completed. File located: ${downloadedFile.toAbsolutePath()}" }

        val media = try {
            mediaInfoExtractor.fetchMediaInfo(url, downloadedFile.toAbsolutePath().toString())
        } catch (e: Exception) {
            log.error(e) { "Failed to extract media info for file=${downloadedFile.toAbsolutePath()}" }
            throw RuntimeException("Failed to extract media info", e)
        }

        log.info { "Media created: ${media.title} (${media.path})" }
        return media
    }

    private fun prepareDownloadsDirectory() {
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
        val host = try {
            URI(url).host?.replace(":", "-") ?: "media"
        } catch (e: Exception) {
            "media"
        }
        val timestamp = Instant.now().epochSecond
        return "$host-$timestamp-${UUID.randomUUID().toString().substring(0, 8)}"
    }

    private fun generateFileTemplate(basePrefix: String) = downloadsRoot.resolve("$basePrefix.%(ext)s").toString()

    private fun findFirstMatchingFile(dir: Path, prefix: String) = Files.list(dir)
        .asSequence()
        .filter { it.isRegularFile() && it.name.startsWith(prefix) }
        .firstOrNull()
}
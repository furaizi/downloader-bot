package com.download.downloaderbot.core.tools.ytdlp

import com.download.downloaderbot.core.config.properties.YtDlpProperties
import com.download.downloaderbot.core.domain.Media
import com.download.downloaderbot.core.domain.MediaType
import com.download.downloaderbot.core.downloader.MediaDownloadException
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
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
class YtDlp(
    val config: YtDlpProperties,
    val mapper: ObjectMapper
) : AbstractCliTool(config.bin) {

    private val downloadsDir = Paths.get(config.baseDir)

    suspend fun download(url: String): Media {
        val basePrefix = generateBasePrefix(url)
        val outputPathTemplate = generateFilePathTemplate(basePrefix)

        val ytDlpMedia = probe(url)
        val args = listOf("-f", config.format, "-o", outputPathTemplate) + config.extraArgs
        execute(url, args)

        val downloadedFile = findDownloadedFileOrThrow(basePrefix)
        val media = Media(
            type = MediaType.fromString(ytDlpMedia.type),
            fileUrl = downloadedFile.toAbsolutePath().toString(),
            sourceUrl = url,
            title = ytDlpMedia.title
        )
        log.info { "yt-dlp download finished: $url -> $outputPathTemplate" }
        return media
    }

    private suspend fun probe(url: String): YtDlpMedia {
        val json = dumpJson(url)
        return mapJsonToInnerMedia(json, url)
    }

    private suspend fun findDownloadedFileOrThrow(basePrefix: String) =
        findLatestMatchingFile(downloadsDir, basePrefix) ?: run {
            val msg = "Downloaded file not found for prefix $basePrefix in $downloadsDir"
            log.error { msg }
            throw RuntimeException(msg)
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



    private fun generateBasePrefix(url: String): String {
        val host = runCatching { URI(url).host?.replace(":", "-") }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }
            ?: "media"
        val timestamp = Instant.now().toEpochMilli()
        val shortUuid = UUID.randomUUID().toString().take(8)
        return "$host-$timestamp-$shortUuid"
    }

    private fun generateFilePathTemplate(basePrefix: String): String =
        downloadsDir.resolve("$basePrefix.%(ext)s").toString()

    private suspend fun dumpJson(url: String): String {
        val args = listOf("--dump-json", "--no-warnings", "--skip-download")
        val raw = execute(url, args)
        return raw.lineSequence()
            .map { it.trim() }
            .firstOrNull { it.startsWith("{") && it.endsWith("}") }
            ?: throw MediaDownloadException("yt-dlp produced no JSON", exitCode = 0, output = raw)
    }

    private fun mapJsonToInnerMedia(json: String, url: String): YtDlpMedia = try {
        mapper.readValue(json, YtDlpMedia::class.java)
    } catch (e: Exception) {
        log.error(e) { "Failed to parse yt-dlp json for url=$url" }
        throw RuntimeException("Failed to parse yt-dlp output", e)
    }

}
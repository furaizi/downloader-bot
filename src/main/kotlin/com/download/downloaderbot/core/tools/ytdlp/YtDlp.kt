package com.download.downloaderbot.core.tools.ytdlp

import com.download.downloaderbot.core.config.properties.YtDlpProperties
import com.download.downloaderbot.core.domain.Media
import com.download.downloaderbot.core.domain.MediaType
import com.download.downloaderbot.core.downloader.MediaDownloadException
import com.download.downloaderbot.core.tools.AbstractCliTool
import com.download.downloaderbot.core.tools.ForYtDlp
import com.download.downloaderbot.core.tools.util.filefinder.FilesByPrefixFinder
import com.download.downloaderbot.core.tools.util.pathgenerator.PathTemplateGenerator
import com.fasterxml.jackson.databind.ObjectMapper
import mu.KotlinLogging
import org.springframework.stereotype.Service
import java.nio.file.Paths

private val log = KotlinLogging.logger {}

@Service
class YtDlp(
    val config: YtDlpProperties,
    val mapper: ObjectMapper,
    @ForYtDlp val pathGenerator: PathTemplateGenerator,
    val fileFinder: FilesByPrefixFinder
) : AbstractCliTool(config.bin) {

    private val downloadsDir = Paths.get(config.baseDir)

    suspend fun download(url: String): Media {
        val (basePrefix, outputPathTemplate) = pathGenerator.generate(url)

        val ytDlpMedia = probe(url)
        val args = listOf("-f", config.format, "-o", outputPathTemplate) + config.extraArgs
        execute(url, args)

        val downloadedFile = fileFinder.find(basePrefix, downloadsDir)
                                        .first()
        val media = Media(
            type = MediaType.fromString(ytDlpMedia.type),
            fileUrl = downloadedFile.toAbsolutePath().toString(),
            sourceUrl = url,
            title = ytDlpMedia.title
        )
        log.info { "yt-dlp download finished: $url -> ${media.fileUrl}" }
        return media
    }

    private suspend fun probe(url: String): YtDlpMedia {
        val json = dumpJson(url)
        return mapJsonToInnerMedia(json, url)
    }

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
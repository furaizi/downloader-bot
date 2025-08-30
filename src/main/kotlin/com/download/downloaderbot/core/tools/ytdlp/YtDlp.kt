package com.download.downloaderbot.core.tools.ytdlp

import com.download.downloaderbot.core.config.properties.YtDlpProperties
import com.download.downloaderbot.core.downloader.MediaDownloadException
import com.download.downloaderbot.core.tools.AbstractCliTool
import com.fasterxml.jackson.databind.ObjectMapper
import mu.KotlinLogging
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

@Service
class YtDlp(
    val config: YtDlpProperties,
    val mapper: ObjectMapper,
) : AbstractCliTool(config.bin) {

    suspend fun download(url: String, outputPathTemplate: String) {
        val formatArgs = if (config.format.isNotEmpty())
            listOf("-f", config.format)
        else emptyList()
        val args = listOf("-o", outputPathTemplate) +
                formatArgs +
                config.extraArgs
        execute(url, args)
    }

    suspend fun probe(url: String): YtDlpMedia {
        val args = listOf("--dump-json", "--no-warnings", "--skip-download")
        val raw = execute(url, args)
        val json = getJson(raw)
        return mapJsonToInnerMedia(json, url)
    }

    private fun getJson(raw: String): String =
        raw.lineSequence()
            .map { it.trim() }
            .firstOrNull { it.startsWith("{") && it.endsWith("}") }
            ?: throw MediaDownloadException("yt-dlp produced no JSON", exitCode = 0, output = raw)

    private fun mapJsonToInnerMedia(json: String, url: String): YtDlpMedia = try {
        mapper.readValue(json, YtDlpMedia::class.java)
    } catch (e: Exception) {
        log.error(e) { "Failed to parse yt-dlp json for url=$url" }
        throw RuntimeException("Failed to parse yt-dlp output", e)
    }
}
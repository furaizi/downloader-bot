package com.download.downloaderbot.infra.process.tools.ytdlp

import com.download.downloaderbot.app.config.properties.YtDlpProperties
import com.download.downloaderbot.core.downloader.MediaDownloaderToolException
import com.download.downloaderbot.infra.process.cli.ytdlp.YtDlpMedia
import com.download.downloaderbot.infra.process.runner.ProcessRunner
import com.fasterxml.jackson.databind.ObjectMapper
import mu.KotlinLogging
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

@Service
class YtDlp(
    val ytDlpExecutor: ProcessRunner,
    val config: YtDlpProperties,
    val mapper: ObjectMapper,
) {

    suspend fun download(url: String, outputPathTemplate: String) {
        val formatArgs = if (config.format.isNotEmpty())
            listOf("-f", config.format)
        else emptyList()
        val args = listOf("-o", outputPathTemplate) +
                formatArgs +
                config.extraArgs +
                url
        ytDlpExecutor.run(args, url)
    }

    suspend fun probe(url: String): YtDlpMedia {
        val args = listOf("--dump-json", "--no-warnings", "--skip-download", url)
        val raw = ytDlpExecutor.run(args, url)
        val json = getJson(raw)
        return mapJsonToInnerMedia(json, url)
    }

    private fun getJson(raw: String): String =
        raw.lineSequence()
            .map { it.trim() }
            .firstOrNull { it.startsWith("{") && it.endsWith("}") }
            ?: throw MediaDownloaderToolException("${config.bin} produced no JSON", output = raw)

    private fun mapJsonToInnerMedia(json: String, url: String): YtDlpMedia = try {
        mapper.readValue(json, YtDlpMedia::class.java)
    } catch (e: Exception) {
        throw RuntimeException("Failed to parse yt-dlp output for url=$url", e)
    }
}
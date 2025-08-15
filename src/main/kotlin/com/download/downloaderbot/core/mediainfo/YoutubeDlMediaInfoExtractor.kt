package com.download.downloaderbot.core.mediainfo

import com.download.downloaderbot.core.entity.Media
import com.download.downloaderbot.core.entity.MediaType
import com.download.downloaderbot.core.entity.YoutubeDlMedia
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

@Component
class YoutubeDlMediaInfoExtractor : MediaInfoExtractor {

    private val mapper = ObjectMapper().registerKotlinModule()

    override suspend fun fetchMediaInfo(url: String, path: String): Media = coroutineScope {
        log.info { "Fetching media info for url=$url path=$path" }
        val json = readJsonFromYtDlp(url)
        val ytDlpMedia = try {
            mapper.readValue(json, YoutubeDlMedia::class.java)
        } catch (e: Exception) {
            log.error(e) { "Failed to parse yt-dlp json for url=$url" }
            throw RuntimeException("Failed to parse yt-dlp output", e)
        }

        val hasAudio = ytDlpMedia.audioCodec != null && ytDlpMedia.audioCodec != "none"
        val media = Media(
            url = url,
            title = ytDlpMedia.title,
            path = path,
            filename = ytDlpMedia.filename,
            quality = ytDlpMedia.height,
            duration = ytDlpMedia.duration,
            platform = ytDlpMedia.extractor,
            type = MediaType.fromString(ytDlpMedia.type),
            hasAudio = hasAudio
        )

        log.info { "Fetched media info: $media" }
        media
    }

    private suspend fun readJsonFromYtDlp(url: String): String = withContext(Dispatchers.IO) {
        val processBuilder = ProcessBuilder(
            "yt-dlp",
            "--dump-json",
            "--no-warnings",
            "--skip-download",
            url
        )
            .redirectErrorStream(true)

        val process = processBuilder.start()

        val stdoutDeferred = let {
            GlobalScope.async(Dispatchers.IO) {
                process.inputStream.bufferedReader().use { it.readText() }
            }
        }

        val exitCode = process.waitFor()
        val output = stdoutDeferred.getCompleted()

        if (exitCode != 0) {
            log.error { "yt-dlp failed (code=$exitCode). Output:\n$output" }
            throw RuntimeException("yt-dlp failed with exit code $exitCode")
        }

        output
    }
}
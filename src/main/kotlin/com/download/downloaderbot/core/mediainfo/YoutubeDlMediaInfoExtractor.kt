package com.download.downloaderbot.core.mediainfo

import com.download.downloaderbot.core.entity.Media
import com.download.downloaderbot.core.entity.MediaType
import com.download.downloaderbot.core.entity.YoutubeDlMedia
import com.download.downloaderbot.core.ytdlp.YtDlp
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
class YoutubeDlMediaInfoExtractor(val ytDlp: YtDlp) : MediaInfoExtractor {

    private val mapper = ObjectMapper().registerKotlinModule()

    override suspend fun fetchMediaInfo(url: String, path: String): Media = coroutineScope {
        require(url.isNotBlank()) { "url must not be blank" }
        require(path.isNotBlank()) { "path must not be blank" }

        log.info { "Fetching media info for url=$url path=$path" }
        val json = ytDlp.dumpJson(url)
        val ytDlpMedia = mapJsonToMedia(json, url)

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

    private fun mapJsonToMedia(json: String, url: String) = try {
            mapper.readValue(json, YoutubeDlMedia::class.java)
        } catch (e: Exception) {
            log.error(e) { "Failed to parse yt-dlp json for url=$url" }
            throw RuntimeException("Failed to parse yt-dlp output", e)
        }
}
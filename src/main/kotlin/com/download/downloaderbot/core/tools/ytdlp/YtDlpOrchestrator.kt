package com.download.downloaderbot.core.tools.ytdlp

import com.download.downloaderbot.core.config.properties.MediaProperties
import com.download.downloaderbot.core.domain.Media
import com.download.downloaderbot.core.domain.MediaType
import com.download.downloaderbot.core.downloader.MediaDownloader
import com.download.downloaderbot.core.downloader.MediaTooLargeException
import com.download.downloaderbot.core.downloader.toMB
import com.download.downloaderbot.core.tools.ForYtDlp
import com.download.downloaderbot.core.tools.util.filefinder.FilesByPrefixFinder
import com.download.downloaderbot.core.tools.util.pathgenerator.PathTemplateGenerator
import mu.KotlinLogging
import org.springframework.stereotype.Component
import org.springframework.util.unit.DataSize

private val log = KotlinLogging.logger {}

@Component
class YtDlpOrchestrator(
    val props: MediaProperties,
    val ytDlp: YtDlp,
    @ForYtDlp val pathGenerator: PathTemplateGenerator,
    @ForYtDlp val fileFinder: FilesByPrefixFinder
) : MediaDownloader {

    private val videoMaxSize = props.maxSize.video

    override suspend fun download(url: String): List<Media> {
        val (basePrefix, outputPathTemplate) = pathGenerator.generate(url)

        val ytDlpMedia = ytDlp.probe(url)
        if (ytDlpMedia.exceeds(videoMaxSize))
            throw MediaTooLargeException(
                actualSize = ytDlpMedia.filesize,
                limit = videoMaxSize.toBytes(),
                mediaType = MediaType.fromString(ytDlpMedia.type)
            )

        ytDlp.download(url, outputPathTemplate)

        return fileFinder.find(basePrefix, props.basePath)
            .onEach { path -> log.info { "yt-dlp download finished: $url -> $path" } }
            .map { path -> ytDlpMedia.toMedia(path.toAbsolutePath().toString(), url) }
    }

    private fun YtDlpMedia.exceeds(limit: DataSize): Boolean {
        if (filesize <= 0 && approximateFileSize <= 0) return true // size unknown
        return filesize > limit.toBytes() || approximateFileSize > limit.toBytes()
    }

    private fun YtDlpMedia.toMedia(filePath: String, sourceUrl: String) = Media(
        type = MediaType.fromString(this.type),
        fileUrl = filePath,
        sourceUrl = sourceUrl,
        title = this.title
    )
}
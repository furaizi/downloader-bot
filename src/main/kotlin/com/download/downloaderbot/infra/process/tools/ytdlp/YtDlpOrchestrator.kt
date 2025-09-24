package com.download.downloaderbot.infra.process.tools.ytdlp

import com.download.downloaderbot.app.config.properties.MediaProperties
import com.download.downloaderbot.core.domain.Media
import com.download.downloaderbot.core.domain.MediaType
import com.download.downloaderbot.core.downloader.MediaDownloader
import com.download.downloaderbot.core.downloader.MediaTooLargeException
import com.download.downloaderbot.infra.process.tools.ForYtDlp
import com.download.downloaderbot.infra.process.tools.util.filefinder.FilesByPrefixFinder
import com.download.downloaderbot.infra.process.tools.util.pathgenerator.PathTemplateGenerator
import mu.KotlinLogging
import org.springframework.stereotype.Component
import org.springframework.util.unit.DataSize
import java.nio.file.Path

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
        val metaData = ytDlp.probe(url)
        requireNotExceedsLimit(metaData, url)

        val (basePrefix, outputPathTemplate) = pathGenerator.generate(url)
        ytDlp.download(url, outputPathTemplate)

        return resolveDownloadedMedia(basePrefix, url, metaData)
    }

    override fun supports(url: String) = true

    private fun requireNotExceedsLimit(meta: YtDlpMedia, url: String) {
        if (meta.exceeds(videoMaxSize)) {
            throw MediaTooLargeException(
                url = url,
                actualSize = meta.filesize,
                limit = videoMaxSize.toBytes()
            )
        }
    }

    private suspend fun resolveDownloadedMedia(basePrefix: String, sourceUrl: String, metaData: YtDlpMedia) =
        fileFinder.find(basePrefix, props.basePath)
            .onEach { path -> log.info { "yt-dlp download finished: $sourceUrl -> $path" } }
            .map { it.toAbsolutePath() }
            .map { path -> metaData.toMedia(path, sourceUrl) }

    private fun YtDlpMedia.exceeds(limit: DataSize): Boolean {
        if (filesize <= 0 && approximateFileSize <= 0) return true // size unknown
        return filesize > limit.toBytes() || approximateFileSize > limit.toBytes()
    }

    private fun YtDlpMedia.toMedia(filePath: Path, sourceUrl: String) = Media(
        type = MediaType.fromString(this.type),
        fileUrl = filePath.toString(),
        sourceUrl = sourceUrl,
        title = this.title
    )
}
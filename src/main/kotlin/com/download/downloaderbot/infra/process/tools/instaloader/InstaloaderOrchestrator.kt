package com.download.downloaderbot.infra.process.tools.instaloader

import com.download.downloaderbot.app.config.properties.MediaProperties
import com.download.downloaderbot.core.domain.Media
import com.download.downloaderbot.core.domain.MediaType
import com.download.downloaderbot.core.downloader.MediaDownloader
import com.download.downloaderbot.infra.process.tools.ForInstaloader
import com.download.downloaderbot.infra.process.tools.util.filefinder.FilesByPrefixFinder
import com.download.downloaderbot.infra.process.tools.util.pathgenerator.PathTemplateGenerator
import org.springframework.stereotype.Component
import java.nio.file.Path

@Component
class InstaloaderOrchestrator(
    val props: MediaProperties,
    val instaloader: Instaloader,
    @ForInstaloader val pathGenerator: PathTemplateGenerator,
    @ForInstaloader val fileFinder: FilesByPrefixFinder
) : MediaDownloader {

    override fun supports(url: String): Boolean {
        return url.contains("instagram.com") || url.contains("instagr.am")
    }

    override suspend fun download(url: String): List<Media> {
        val (basePrefix, outputPathTemplate) = pathGenerator.generate(url)
        val metaData = instaloader.probe(url, outputPathTemplate)
        instaloader.download(url, outputPathTemplate)
        return resolveDownloadedMedia(basePrefix, url, metaData)
    }

    private suspend fun resolveDownloadedMedia(basePrefix: String, sourceUrl: String, metaData: InstaloaderMedia) =
        fileFinder.find(basePrefix, props.basePath)
            .onEach { path -> println("instaloader download finished: $sourceUrl -> $path") }
            .map { it.toAbsolutePath() }
            .map { path -> metaData.toMedia(path, sourceUrl) }

    private fun InstaloaderMedia.toMedia(filePath: Path, sourceUrl: String) = Media(
        type = MediaType.VIDEO,
        fileUrl = filePath.toString(),
        sourceUrl = sourceUrl,
        title = this.node.title,
    )
}
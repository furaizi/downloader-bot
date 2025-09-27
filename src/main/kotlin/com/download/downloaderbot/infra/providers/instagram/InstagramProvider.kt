package com.download.downloaderbot.infra.providers.instagram

import com.download.downloaderbot.app.config.properties.MediaProperties
import com.download.downloaderbot.core.domain.Media
import com.download.downloaderbot.core.domain.MediaType
import com.download.downloaderbot.core.downloader.MediaProvider
import com.download.downloaderbot.infra.process.cli.api.CliTool
import com.download.downloaderbot.infra.process.cli.instaloader.InstaloaderMedia
import com.download.downloaderbot.infra.process.tools.ForInstaloader
import com.download.downloaderbot.infra.process.tools.instaloader.Instaloader
import com.download.downloaderbot.infra.providers.interfaces.FilesByPrefixFinder
import com.download.downloaderbot.infra.providers.interfaces.PathTemplateGenerator
import com.download.downloaderbot.infra.providers.util.toMedia
import org.springframework.stereotype.Component
import java.nio.file.Path

@Component
class InstagramProvider(
    val props: MediaProperties,
    val instaloader: CliTool<InstaloaderMedia>,
    @ForInstaloader val pathGenerator: PathTemplateGenerator,
    @ForInstaloader val fileFinder: FilesByPrefixFinder
) : MediaProvider {

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
            .map { path -> metaData.toMedia(path, sourceUrl) }

}
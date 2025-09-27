package com.download.downloaderbot.infra.providers.gallery

import com.download.downloaderbot.app.config.properties.MediaProperties
import com.download.downloaderbot.core.domain.Media
import com.download.downloaderbot.core.domain.MediaType
import com.download.downloaderbot.core.downloader.MediaProvider
import com.download.downloaderbot.infra.process.cli.api.CliTool
import com.download.downloaderbot.infra.process.cli.gallerydl.GalleryDlMedia
import com.download.downloaderbot.infra.process.tools.ForGalleryDl
import com.download.downloaderbot.infra.process.tools.gallerydl.GalleryDl
import com.download.downloaderbot.infra.providers.interfaces.FilesByPrefixFinder
import com.download.downloaderbot.infra.providers.interfaces.PathTemplateGenerator
import com.download.downloaderbot.infra.providers.util.toMedia
import mu.KotlinLogging
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.nio.file.Path

private val log = KotlinLogging.logger {}

@ConditionalOnProperty(
    prefix = "downloader.gallery-dl",
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = false
)
@Component
class GalleryProvider(
    val props: MediaProperties,
    val galleryDl: CliTool<GalleryDlMedia>,
    @ForGalleryDl val pathGenerator: PathTemplateGenerator,
    @ForGalleryDl val fileFinder: FilesByPrefixFinder
) : MediaProvider {

    override suspend fun download(url: String): List<Media> {
        val (folderName, outputPath) = pathGenerator.generate(url)

        val galleryDlMedia = galleryDl.probe(url)
        galleryDl.download(url, outputPath)

        return fileFinder.find(folderName, props.basePath)
            .onEachIndexed { i, path -> log.info { "[$i] gallery-dl download finished: $url -> $path" } }
            .map { path -> galleryDlMedia.toMedia(path, url) }
    }

    override fun supports(url: String) = false

}
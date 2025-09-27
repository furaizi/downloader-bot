package com.download.downloaderbot.infra.process.tools.gallerydl

import com.download.downloaderbot.app.config.properties.MediaProperties
import com.download.downloaderbot.core.domain.Media
import com.download.downloaderbot.core.domain.MediaType
import com.download.downloaderbot.core.downloader.MediaProvider
import com.download.downloaderbot.infra.process.tools.ForGalleryDl
import com.download.downloaderbot.infra.process.tools.util.filefinder.FilesByPrefixFinder
import com.download.downloaderbot.infra.process.tools.util.pathgenerator.PathTemplateGenerator
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
class GalleryDlOrchestrator(
    val props: MediaProperties,
    val galleryDl: GalleryDl,
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

    private fun GalleryDlMedia.toMedia(filePath: Path, sourceUrl: String) = Media(
        type = MediaType.IMAGE,
        fileUrl = filePath.toAbsolutePath().toString(),
        sourceUrl = sourceUrl,
        title = this.title
    )

}
package com.download.downloaderbot.core.tools.gallerydl

import com.download.downloaderbot.core.config.properties.MediaProperties
import com.download.downloaderbot.core.domain.Media
import com.download.downloaderbot.core.domain.MediaType
import com.download.downloaderbot.core.downloader.MediaDownloader
import com.download.downloaderbot.core.tools.ForGalleryDl
import com.download.downloaderbot.core.tools.util.filefinder.FilesByPrefixFinder
import com.download.downloaderbot.core.tools.util.pathgenerator.PathTemplateGenerator
import mu.KotlinLogging
import org.springframework.stereotype.Component
import java.nio.file.Path

private val log = KotlinLogging.logger {}

@Component
class GalleryDlOrchestrator(
    val props: MediaProperties,
    val galleryDl: GalleryDl,
    @ForGalleryDl val pathGenerator: PathTemplateGenerator,
    @ForGalleryDl val fileFinder: FilesByPrefixFinder
) : MediaDownloader {

    override suspend fun download(url: String): List<Media> {
        val (folderName, outputPath) = pathGenerator.generate(url)

        val galleryDlMedia = galleryDl.probe(url)
        galleryDl.download(url, outputPath)

        return fileFinder.find(folderName, props.basePath)
            .onEachIndexed { i, path -> log.info { "[$i] gallery-dl download finished: $url -> $path" } }
            .map { path -> galleryDlMedia.toMedia(path, url) }
    }

    private fun GalleryDlMedia.toMedia(filePath: Path, sourceUrl: String) = Media(
        type = MediaType.IMAGE,
        fileUrl = filePath.toAbsolutePath().toString(),
        sourceUrl = sourceUrl,
        title = this.title
    )

}
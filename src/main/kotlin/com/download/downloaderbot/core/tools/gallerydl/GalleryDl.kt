package com.download.downloaderbot.core.tools.gallerydl

import com.download.downloaderbot.core.config.properties.GalleryDlProperties
import com.download.downloaderbot.core.domain.Media
import com.download.downloaderbot.core.domain.MediaType
import com.download.downloaderbot.core.tools.AbstractCliTool
import com.download.downloaderbot.core.tools.util.filefinder.FileByPrefixFinder
import com.download.downloaderbot.core.tools.util.pathgenerator.PathTemplateGenerator
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service
import java.nio.file.Paths

@Service
class GalleryDl(
    val config: GalleryDlProperties,
    val mapper: ObjectMapper,
    val pathGenerator: PathTemplateGenerator,
    val fileFinder: FileByPrefixFinder
) : AbstractCliTool(config.bin) {
    private val downloadsDir = Paths.get(config.baseDir)

    suspend fun download(url: String): List<Media> {
        val (folderName, outputPath) = pathGenerator.generate(url)

        val galleryDlMedia = probe(url)
        val args = listOf("-D", outputPath,
            "-f", "{num}.{extension}",
            "--filter", "type == 'image'") +
                config.extraArgs
        execute(url, args)

        val downloadedFiles = fileFinder.find(folderName, downloadsDir)
        return downloadedFiles.map { filePath ->
            Media(
                type = MediaType.IMAGE,
                fileUrl = filePath.toAbsolutePath().toString(),
                sourceUrl = url,
                title = "",
            )
        }

    }


    private suspend fun probe(url: String) {}
}
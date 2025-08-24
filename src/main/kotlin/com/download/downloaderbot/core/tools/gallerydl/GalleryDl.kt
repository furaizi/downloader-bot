package com.download.downloaderbot.core.tools.gallerydl

import com.download.downloaderbot.core.config.properties.GalleryDlProperties
import com.download.downloaderbot.core.tools.AbstractCliTool
import com.download.downloaderbot.core.tools.ForGalleryDl
import com.download.downloaderbot.core.tools.util.filefinder.FilesByPrefixFinder
import com.download.downloaderbot.core.tools.util.pathgenerator.PathTemplateGenerator
import org.springframework.stereotype.Service
import java.nio.file.Paths

@Service
class GalleryDl(
    val config: GalleryDlProperties
) : AbstractCliTool(config.bin) {

    suspend fun download(url: String, outputPath: String) {
        val args = listOf("-D", outputPath,
            "-f", "{num}.{extension}",
            "--filter", "type == 'image'") +
            config.extraArgs
        execute(url, args)
    }

    suspend fun probe(url: String): GalleryDlMedia = GalleryDlMedia()

}
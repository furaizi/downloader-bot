package com.download.downloaderbot.core.downloader.tools.gallerydl

import com.download.downloaderbot.core.config.properties.GalleryDlProperties
import com.download.downloaderbot.core.downloader.tools.AbstractCliTool
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service

@ConditionalOnProperty(
    prefix = "downloader.gallery-dl",
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = false
)
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
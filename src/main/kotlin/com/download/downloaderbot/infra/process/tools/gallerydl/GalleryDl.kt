package com.download.downloaderbot.infra.process.tools.gallerydl

import com.download.downloaderbot.app.config.properties.GalleryDlProperties
import com.download.downloaderbot.infra.process.tools.ProcessRunner
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
    val galleryDlExecutor: ProcessRunner,
    val config: GalleryDlProperties
) {

    suspend fun download(url: String, outputPath: String) {
        val args = listOf("-D", outputPath,
            "-f", "{num}.{extension}",
            "--filter", "type == 'image'", url) +
            config.extraArgs
        galleryDlExecutor.run(args, url)
    }

    suspend fun probe(url: String): GalleryDlMedia = GalleryDlMedia()

}
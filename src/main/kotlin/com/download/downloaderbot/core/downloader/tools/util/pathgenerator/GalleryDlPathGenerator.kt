package com.download.downloaderbot.core.downloader.tools.util.pathgenerator

import com.download.downloaderbot.core.config.properties.MediaProperties
import com.download.downloaderbot.core.downloader.tools.ForGalleryDl
import com.download.downloaderbot.core.downloader.tools.util.baseprefix.BasePrefixGenerator
import org.springframework.stereotype.Component

@Component
@ForGalleryDl
class GalleryDlPathGenerator(
    val props: MediaProperties
) : PathTemplateGenerator {
    override fun generate(url: String): DownloadPath {
        val basePrefix = BasePrefixGenerator.generate(url)
        val outputPath = props.basePath.resolve(basePrefix).toString()
        return DownloadPath(basePrefix, outputPath)
    }
}
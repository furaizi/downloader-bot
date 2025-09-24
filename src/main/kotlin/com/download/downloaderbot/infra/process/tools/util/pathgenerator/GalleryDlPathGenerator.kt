package com.download.downloaderbot.infra.process.tools.util.pathgenerator

import com.download.downloaderbot.app.config.properties.MediaProperties
import com.download.downloaderbot.infra.process.tools.ForGalleryDl
import com.download.downloaderbot.infra.process.tools.util.baseprefix.BasePrefixGenerator
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
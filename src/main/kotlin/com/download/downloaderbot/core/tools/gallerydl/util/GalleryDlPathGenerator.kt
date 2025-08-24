package com.download.downloaderbot.core.tools.gallerydl.util

import com.download.downloaderbot.core.config.properties.MediaProperties
import com.download.downloaderbot.core.tools.ForGalleryDl
import com.download.downloaderbot.core.tools.util.baseprefix.BasePrefixGenerator
import com.download.downloaderbot.core.tools.util.pathgenerator.DownloadPath
import com.download.downloaderbot.core.tools.util.pathgenerator.PathTemplateGenerator
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
package com.download.downloaderbot.infra.providers.common.pathgenerator

import com.download.downloaderbot.app.config.properties.MediaProperties
import com.download.downloaderbot.infra.process.tools.ForGalleryDl
import com.download.downloaderbot.infra.providers.interfaces.DownloadPath
import com.download.downloaderbot.infra.providers.interfaces.PathGenerator
import com.download.downloaderbot.infra.providers.util.BasePrefixGenerator
import org.springframework.stereotype.Component

@Component
class GalleryDlPathGenerator(
    val props: MediaProperties
) : PathGenerator {
    override fun generate(url: String): DownloadPath {
        val basePrefix = BasePrefixGenerator.generate(url)
        val outputPath = props.basePath.resolve(basePrefix).toString()
        return DownloadPath(basePrefix, outputPath)
    }
}
package com.download.downloaderbot.infra.media.path

import com.download.downloaderbot.app.config.properties.MediaProperties
import com.download.downloaderbot.infra.media.path.naming.BasePrefixGenerator
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
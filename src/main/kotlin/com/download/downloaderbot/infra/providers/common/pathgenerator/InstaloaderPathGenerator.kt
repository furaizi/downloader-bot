package com.download.downloaderbot.infra.providers.common.pathgenerator

import com.download.downloaderbot.app.config.properties.MediaProperties
import com.download.downloaderbot.infra.process.tools.ForInstaloader
import com.download.downloaderbot.infra.providers.interfaces.DownloadPath
import com.download.downloaderbot.infra.providers.interfaces.PathTemplateGenerator
import com.download.downloaderbot.infra.providers.util.BasePrefixGenerator
import org.springframework.stereotype.Component

@Component
@ForInstaloader
class InstaloaderPathGenerator(
    val props: MediaProperties
) : PathTemplateGenerator {
    override fun generate(url: String): DownloadPath {
        val basePrefix = BasePrefixGenerator.generate(url)
        val outputTemplate = props.basePath.resolve(basePrefix).toString()
        return DownloadPath(basePrefix, outputTemplate)
    }

}
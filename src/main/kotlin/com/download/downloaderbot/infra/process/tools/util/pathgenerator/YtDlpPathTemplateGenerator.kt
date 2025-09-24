package com.download.downloaderbot.infra.process.tools.util.pathgenerator

import com.download.downloaderbot.app.config.properties.MediaProperties
import com.download.downloaderbot.infra.process.tools.ForYtDlp
import com.download.downloaderbot.infra.process.tools.util.baseprefix.BasePrefixGenerator
import org.springframework.stereotype.Component

@Component
@ForYtDlp
class YtDlpPathTemplateGenerator(
    val props: MediaProperties
) : PathTemplateGenerator {
    override fun generate(url: String): DownloadPath {
        val basePrefix = BasePrefixGenerator.generate(url)
        val outputTemplate = props.basePath.resolve("$basePrefix.%(ext)s")
            .toString()
        return DownloadPath(basePrefix, outputTemplate)
    }
}
package com.download.downloaderbot.core.tools.ytdlp.util

import com.download.downloaderbot.core.config.properties.MediaProperties
import com.download.downloaderbot.core.tools.ForYtDlp
import com.download.downloaderbot.core.tools.util.baseprefix.BasePrefixGenerator
import com.download.downloaderbot.core.tools.util.pathgenerator.DownloadPath
import com.download.downloaderbot.core.tools.util.pathgenerator.PathTemplateGenerator
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
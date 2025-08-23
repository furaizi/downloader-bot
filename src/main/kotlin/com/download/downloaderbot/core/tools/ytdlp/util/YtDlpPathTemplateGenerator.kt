package com.download.downloaderbot.core.tools.ytdlp.util

import com.download.downloaderbot.core.config.properties.YtDlpProperties
import com.download.downloaderbot.core.tools.util.baseprefix.BasePrefixGenerator
import com.download.downloaderbot.core.tools.util.pathgenerator.DownloadPath
import com.download.downloaderbot.core.tools.util.pathgenerator.PathTemplateGenerator
import org.springframework.stereotype.Component
import java.net.URI
import java.nio.file.Paths
import java.time.Instant
import java.util.UUID

@Component
class YtDlpPathTemplateGenerator(
    val props: YtDlpProperties
) : PathTemplateGenerator {

    private val baseDir = Paths.get(props.baseDir)

    override fun generate(url: String): DownloadPath {
        val basePrefix = BasePrefixGenerator.generate(url)
        val outputTemplate = baseDir.resolve("$basePrefix.%(ext)s")
            .toString()
        return DownloadPath(basePrefix, outputTemplate)
    }
}
package com.download.downloaderbot.core.tools.ytdlp.util

import com.download.downloaderbot.core.config.properties.YtDlpProperties
import com.download.downloaderbot.core.tools.util.DownloadPath
import com.download.downloaderbot.core.tools.util.PathTemplateGenerator
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
        val basePrefix = generateBasePrefix(url)
        val outputTemplate = baseDir.resolve("$basePrefix.%(ext)s")
            .toString()
        return DownloadPath(basePrefix, outputTemplate)
    }

    private fun generateBasePrefix(url: String): String {
        val host = getHostName(url) ?: "media"
        val timestamp = Instant.now().toEpochMilli()
        val shortUuid = UUID.randomUUID()
            .toString()
            .take(8)
        return "$host-$timestamp-$shortUuid"
    }

    private fun getHostName(url: String): String? {
        return runCatching {
            URI(url).host
                ?.replace(oldValue = ":", newValue = "-")
        }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }
    }
}
package com.download.downloaderbot.infra.media.provider

import com.download.downloaderbot.core.domain.Media
import com.download.downloaderbot.core.downloader.MediaProvider
import com.download.downloaderbot.core.downloader.UnsupportedSourceException
import com.download.downloaderbot.infra.process.cli.api.ToolRegistry
import com.download.downloaderbot.infra.source.SourceRegistry
import mu.KotlinLogging
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

@Component
class DynamicMediaProvider(
    val sources: SourceRegistry,
    val tools: ToolRegistry
) : MediaProvider {

    override fun supports(url: String): Boolean =
        sources.match(url) != null

    override suspend fun download(url: String): List<Media> {
        val match = sources.match(url) ?: throw UnsupportedSourceException(url)
        val tool = tools.get(match.tool)
        log.info { "Matched url=$url -> source=${match.source}, subresource=${match.subresource}, tool=${match.tool}" }
        return tool.download(url)
    }
}
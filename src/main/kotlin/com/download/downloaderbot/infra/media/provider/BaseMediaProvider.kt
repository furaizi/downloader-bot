package com.download.downloaderbot.infra.media.provider

import com.download.downloaderbot.core.domain.Media
import com.download.downloaderbot.core.downloader.MediaProvider
import com.download.downloaderbot.infra.process.cli.api.CliTool
import com.download.downloaderbot.infra.process.cli.api.MediaConvertible
import mu.KotlinLogging

private val log = KotlinLogging.logger {}

class BaseMediaProvider(
    val tool: CliTool<MediaConvertible>,
    val urlPredicate: (String) -> Boolean,
) : MediaProvider {
    val toolName = tool.toolId.label

    override fun supports(url: String): Boolean =
        urlPredicate(url)
            .also { log.debug { "Checking support for URL=$url with tool=$toolName: $it" } }

    override suspend fun download(url: String): List<Media> =
        tool.download(url)

}

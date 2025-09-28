package com.download.downloaderbot.app.download

import com.download.downloaderbot.core.domain.Media
import com.download.downloaderbot.core.downloader.MediaProvider
import com.download.downloaderbot.core.downloader.UnsupportedSourceException
import mu.KotlinLogging

private val log = KotlinLogging.logger {}

class DefaultMediaService(
    private val providers: Map<String, MediaProvider>
) : MediaService {

    override suspend fun download(url: String): List<Media> {
        return providers.entries.firstOrNull { it.value.supports(url) }
            ?.also { log.info { "Selected provider=${it.key} for url=$url" } }
            ?.value
            ?.download(url)
            ?: throw UnsupportedSourceException(url)

    }
}

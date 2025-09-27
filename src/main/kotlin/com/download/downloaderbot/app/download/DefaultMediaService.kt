package com.download.downloaderbot.app.download

import com.download.downloaderbot.core.domain.Media
import com.download.downloaderbot.core.downloader.MediaProvider
import com.download.downloaderbot.core.downloader.UnsupportedSourceException
import mu.KotlinLogging

private val log = KotlinLogging.logger {}

class DefaultMediaService(
    private val providers: List<MediaProvider>
) : MediaService {

    override suspend fun download(url: String): List<Media> {
        // temp solution because we have only one downloader now
        return providers.firstOrNull { it.supports(url) }
            ?.download(url)
            ?: throw UnsupportedSourceException(url)

    }
}

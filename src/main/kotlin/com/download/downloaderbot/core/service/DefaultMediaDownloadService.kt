package com.download.downloaderbot.core.service

import com.download.downloaderbot.core.domain.Media
import com.download.downloaderbot.core.downloader.MediaDownloader
import com.download.downloaderbot.core.downloader.UnsupportedSourceException
import mu.KotlinLogging
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

@Service
class DefaultMediaDownloadService(
    private val downloaders: List<MediaDownloader>
) : MediaDownloadService {

    override suspend fun download(url: String): List<Media> {
        // temp solution because we have only one downloader now
        return downloaders.firstOrNull { it.supports(url) }
            ?.download(url)
            ?: throw UnsupportedSourceException(url)

    }
}
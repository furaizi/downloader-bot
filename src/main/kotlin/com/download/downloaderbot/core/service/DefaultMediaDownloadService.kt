package com.download.downloaderbot.core.service

import com.download.downloaderbot.core.domain.Media
import com.download.downloaderbot.core.downloader.MediaDownloader
import mu.KotlinLogging
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

@Service
class DefaultMediaDownloadService(
    private val downloaders: List<MediaDownloader>
) : MediaDownloadService {

    override suspend fun download(url: String): List<Media> {
        downloaders.forEach { downloader ->
            runCatching {
                return downloader.download(url)
            }
            .onFailure {
                log.warn(it) { "Downloader ${downloader::class.simpleName} failed for URL: $url" }
            }
        }
        error("No downloader succeeded for URL: $url")
    }
}
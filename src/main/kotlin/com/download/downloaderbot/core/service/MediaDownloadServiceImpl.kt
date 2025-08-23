package com.download.downloaderbot.core.service

import com.download.downloaderbot.core.domain.Media
import com.download.downloaderbot.core.downloader.MediaDownloader
import org.springframework.stereotype.Service

@Service
class MediaDownloadServiceImpl(
    private val downloaders: List<MediaDownloader>
) : MediaDownloadService {

    override suspend fun download(url: String): Media {
        val downloader = downloaders.find { it.supports(url) }
            ?: error("No downloader found for URL: $url")
        return downloader.download(url)
    }
}
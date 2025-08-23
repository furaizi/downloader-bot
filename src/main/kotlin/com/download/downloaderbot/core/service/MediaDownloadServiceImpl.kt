package com.download.downloaderbot.core.service

import com.download.downloaderbot.core.domain.Media
import com.download.downloaderbot.core.downloader.MediaDownloader
import org.springframework.stereotype.Service

@Service
class MediaDownloadServiceImpl(
    private val downloaders: List<MediaDownloader>
) : MediaDownloadService {

    override suspend fun download(url: String): List<Media> {
        downloaders.forEach {
            runCatching {
                return it.download(url)
            }
        }
        error("No downloader succeeded for URL: $url")
    }
}
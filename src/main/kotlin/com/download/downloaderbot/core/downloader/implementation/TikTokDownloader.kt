package com.download.downloaderbot.core.downloader.implementation

import com.download.downloaderbot.core.domain.Media
import com.download.downloaderbot.core.downloader.MediaDownloader
import com.download.downloaderbot.core.tools.ytdlp.YtDlp
import org.springframework.stereotype.Component
import java.net.URI

@Component
class TikTokDownloader(
    private val ytDlp: YtDlp
) : MediaDownloader {

    override fun supports(url: String): Boolean {
        val host = runCatching {
            URI(url).host
                .orEmpty()
                .lowercase()
        }
            .getOrDefault("")
        return host.contains("tiktok.com")
    }

    override suspend fun download(url: String): List<Media> =
        ytDlp.download(url)
}
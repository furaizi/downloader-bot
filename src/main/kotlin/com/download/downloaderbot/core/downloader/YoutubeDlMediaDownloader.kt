package com.download.downloaderbot.core.downloader

import com.download.downloaderbot.core.ytdlp.YtDlp
import kotlinx.coroutines.coroutineScope
import mu.KotlinLogging
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

@Component
class YoutubeDlMediaDownloader(
    val ytDlp: YtDlp
) : MediaDownloader {

    override suspend fun download(url: String, outputPath: String) = coroutineScope {
        require(url.isNotBlank()) { "url must not be blank" }
        require(outputPath.isNotBlank()) { "outputPath must not be blank" }

        log.info { "Starting download for url=$url to outputPath=$outputPath" }
        ytDlp.download(url, outputPath)
    }
}
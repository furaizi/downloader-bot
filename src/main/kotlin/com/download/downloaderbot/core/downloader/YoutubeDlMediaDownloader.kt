package com.download.downloaderbot.core.downloader

import com.download.downloaderbot.core.config.YtDlpConfig
import com.download.downloaderbot.core.ytdlp.YtDlp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import org.springframework.stereotype.Component
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

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
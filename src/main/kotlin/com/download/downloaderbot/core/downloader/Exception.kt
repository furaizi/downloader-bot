package com.download.downloaderbot.core.downloader

import com.download.downloaderbot.core.domain.MediaType

private const val bytesInMB = 1024 * 1024
fun Long.toMB() = this / bytesInMB


open class MediaDownloaderToolException(
    message: String,
    val exitCode: Int = 0,
    val output: String = ""
) : RuntimeException(message)

open class MediaDownloaderException(
    val url: String,
    message: String
) : RuntimeException(message)

class MediaTooLargeException(
    url: String,
    val actualSize: Long,
    val limit: Long,
) : MediaDownloaderException(url,
    "Media file by URL $url exceeds the size limit. " +
            "Actual size: ${actualSize.toMB()} MB, Limit: ${limit.toMB()} MB"
)
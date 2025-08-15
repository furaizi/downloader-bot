package com.download.downloaderbot.core.downloader

class MediaDownloadException(
    message: String,
    val exitCode: Int,
    val output: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)
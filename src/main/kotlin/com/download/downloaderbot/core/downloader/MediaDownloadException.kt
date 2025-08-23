package com.download.downloaderbot.core.downloader

class MediaDownloadException(
    message: String,
    val exitCode: Int = 0,
    val output: String = ""
) : RuntimeException(message) {
}
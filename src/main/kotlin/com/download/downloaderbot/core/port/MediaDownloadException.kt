package com.download.downloaderbot.core.port

class MediaDownloadException(
    message: String,
    val exitCode: Int = 0,
    val output: String = ""
) : RuntimeException(message) {
}
package com.download.downloaderbot.core.downloader


private const val bytesInMB = 1024 * 1024
fun Long.toMB() = this / bytesInMB


open class MediaDownloaderToolException(
    message: String,
    val output: String = "")
    : RuntimeException(message)

class ToolExecutionException(
    val tool: String,
    val exitCode: Int,
    output: String
) : MediaDownloaderToolException("$tool failed (code=$exitCode): $output", output)


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

class MediaNotFoundException(url: String)
    : MediaDownloaderException(url, "No media found at URL: $url")

class UnsupportedSourceException(url: String)
    : MediaDownloaderException(url, "No downloader supports the URL: $url")

class DownloadInProgressException(url: String) :
    MediaDownloaderException(url, "Download already in progress for url=$url")

class BusyException(url: String) :
    MediaDownloaderException(url, "Downloader is busy now, try again later for url=$url")
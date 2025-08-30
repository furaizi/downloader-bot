package com.download.downloaderbot.core.downloader

import com.download.downloaderbot.core.domain.MediaType

private const val bytesInMB = 1024 * 1024
fun Long.toMB() = this / bytesInMB

class MediaTooLargeException(
    val actualSize: Long,
    val limit: Long,
    val mediaType: MediaType
) : RuntimeException(
    "Media file of type $mediaType exceeds the size limit. " +
            "Actual size: ${actualSize.toMB()} MB, Limit: ${limit.toMB()} bytes"
)
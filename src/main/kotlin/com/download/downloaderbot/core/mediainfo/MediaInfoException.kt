package com.download.downloaderbot.core.mediainfo

class MediaInfoException(
    message: String,
    val exitCode: Int? = null,
    val output: String? = null,
    cause: Throwable? = null
) : RuntimeException(message, cause)
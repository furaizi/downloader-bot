package com.download.downloaderbot.core.cache

import com.download.downloaderbot.core.domain.Media

data class CachedMedia(
    val media: Media,
    val fileUniqueId: String,
    val lastFileId: String
)

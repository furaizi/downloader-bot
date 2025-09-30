package com.download.downloaderbot.core.cache

import com.download.downloaderbot.core.domain.MediaType

data class CachedMedia(
    val type: MediaType,
    val fileUniqueId: String,
    val lastFileId: String
)

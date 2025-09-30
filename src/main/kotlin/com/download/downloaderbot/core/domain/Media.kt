package com.download.downloaderbot.core.domain

import java.time.OffsetDateTime

data class Media(
    val type: MediaType,
    val fileUrl: String,
    val sourceUrl: String,
    val title: String?,
    val fileUniqueId: String? = null,
    val lastFileId: String? = null,
    val downloadedAt: OffsetDateTime = OffsetDateTime.now()
)

enum class MediaType {
    VIDEO,
    AUDIO,
    IMAGE;

    companion object {
        fun fromString(type: String) = valueOf(type.uppercase())
    }
}

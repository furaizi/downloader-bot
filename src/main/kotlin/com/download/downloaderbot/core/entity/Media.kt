package com.download.downloaderbot.core.entity

import java.time.OffsetDateTime

data class Media(
    val url: String,
    val title: String,
    val path: String,
    val filename: String,
    val quality: Int,
    val duration: Long,
    val platform: String,
    val type: MediaType,
    val hasAudio: Boolean = false
)

data class NewMedia(
    val type: MediaType,
    val fileUrl: String,
    val sourceUrl: String,
    val title: String?,
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

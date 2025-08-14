package com.download.downloaderbot.core.entity

data class Media(
    val url: String,
    val title: String,
    val path: String,
    val quality: String,
    val duration: String,
    val platform: String,
    val type: MediaType,
    val hasAudio: Boolean = false
)

enum class MediaType {
    VIDEO,
    AUDIO,
    IMAGE;

    companion object {
        fun fromString(type: String) = valueOf(type.uppercase())
    }
}

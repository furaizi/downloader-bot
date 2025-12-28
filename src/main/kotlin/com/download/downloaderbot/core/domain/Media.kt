package com.download.downloaderbot.core.domain

import java.nio.file.Path
import java.time.OffsetDateTime

data class Media(
    val type: MediaType,
    val fileUrl: String,
    val sourceUrl: String,
    val title: String?,
    val fileUniqueId: String? = null,
    val lastFileId: String? = null,
    val downloadedAt: OffsetDateTime = OffsetDateTime.now(),
)

enum class MediaType {
    VIDEO,
    AUDIO,
    IMAGE,
    ;

    companion object {
        fun fromString(type: String) = valueOf(type.uppercase())

        fun fromPath(path: Path): MediaType? {
            val extension = path.toString()
                .substringAfterLast('.', "")
                .lowercase()
            return when (extension) {
                "jpg", "jpeg", "png", "webp", "gif", "bmp", "tiff", "heic" -> IMAGE
                "mp4", "m4v", "mov", "webm", "mkv", "avi", "flv" -> VIDEO
                "mp3", "wav", "aac", "flac", "ogg", "m4a", "opus" -> AUDIO
                else -> null
            }
        }
    }
}

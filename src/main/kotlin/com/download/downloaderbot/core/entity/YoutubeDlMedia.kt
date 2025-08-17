package com.download.downloaderbot.core.entity

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class YoutubeDlMedia(
    val title: String,
    val filename: String,
    val resolution: String,
    val duration: Long, // in seconds
    val width: Int,
    val height: Int,
    val filesize: Long, // in bytes
    val extractor: String,
    val uploader: String,
    @JsonProperty("_type") val type: String,
    @JsonProperty("ext") val extension: String,
    val hasAudio: Boolean = false,
    @JsonProperty("vcodec") val videoCodec: String?,
    @JsonProperty("acodec") val audioCodec: String?
)

package com.download.downloaderbot.core.tools.ytdlp

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class YtDlpMedia(
    val title: String,
    val filename: String,
    val resolution: String,
    val duration: Long, // in seconds
    val width: Int,
    val height: Int,
    val filesize: Long = 0, // in bytes
    @JsonProperty("filesize_approx")
    val approximateFileSize: Long = 0,
    val extractor: String,
    val uploader: String,
    @JsonProperty("_type") val type: String,
    @JsonProperty("ext") val extension: String,
    val hasAudio: Boolean = false,
    @JsonProperty("vcodec") val videoCodec: String?,
    @JsonProperty("acodec") val audioCodec: String?
)
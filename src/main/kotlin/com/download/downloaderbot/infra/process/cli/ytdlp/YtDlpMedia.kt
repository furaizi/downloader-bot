package com.download.downloaderbot.infra.process.cli.ytdlp

import com.download.downloaderbot.core.domain.Media
import com.download.downloaderbot.core.domain.MediaType
import com.download.downloaderbot.infra.process.cli.api.MediaConvertible
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import java.nio.file.Path

@JsonIgnoreProperties(ignoreUnknown = true)
data class YtDlpMedia(
    val title: String,
    val filename: String,
    val resolution: String,
    // in seconds
    val duration: Long,
    val width: Int,
    val height: Int,
    // in bytes
    val filesize: Long = 0,
    @JsonProperty("filesize_approx")
    val approximateFileSize: Long = 0,
    val extractor: String,
    val uploader: String,
    @JsonProperty("_type") val type: String,
    @JsonProperty("ext") val extension: String,
    val hasAudio: Boolean = false,
    @JsonProperty("vcodec") val videoCodec: String?,
    @JsonProperty("acodec") val audioCodec: String?,
) : MediaConvertible {
    override fun toMedia(
        filePath: Path,
        sourceUrl: String,
    ) = Media(
        type = MediaType.fromString(this.type),
        fileUrl = filePath.toAbsolutePath().toString(),
        sourceUrl = sourceUrl,
        title = this.title,
    )
}

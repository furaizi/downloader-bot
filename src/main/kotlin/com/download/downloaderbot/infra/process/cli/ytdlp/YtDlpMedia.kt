package com.download.downloaderbot.infra.process.cli.ytdlp

import com.download.downloaderbot.core.domain.Media
import com.download.downloaderbot.core.domain.MediaType
import com.download.downloaderbot.infra.process.cli.api.MediaConvertible
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import java.nio.file.Path

// I have observed that the bitrate-based estimation is on average
// about 2x higher than the actual file size for YouTube videos.
private const val BITRATE_SIZE_ADJUSTMENT = 0.555

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
    @JsonProperty("tbr") val totalBitrateKbps: Double = 0.0,
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

    override fun mediaType() = MediaType.fromString(this.type)

    override fun estimatedSizeBytes(): Long? {
        if (filesize > 0) {
            return filesize
        }

        if (approximateFileSize > 0) {
            return approximateFileSize
        }

        if (duration > 0 && totalBitrateKbps > 0.0) {
            val bitsPerSecond = totalBitrateKbps * 1000.0
            val totalBits = bitsPerSecond * duration
            val rawBytes = totalBits / 8.0

            // Empirical coefficient
            return (rawBytes * BITRATE_SIZE_ADJUSTMENT).toLong()
        }

        return null
    }
}

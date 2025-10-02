package com.download.downloaderbot.infra.process.cli.instaloader

import com.download.downloaderbot.core.domain.Media
import com.download.downloaderbot.core.domain.MediaType
import com.download.downloaderbot.infra.process.cli.api.MediaConvertible
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import java.nio.file.Path

@JsonIgnoreProperties(ignoreUnknown = true)
data class InstaloaderMedia(
    val node: Node,
) : MediaConvertible {
    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Node(
        val title: String,
        val shortcode: String,
        @JsonProperty("video_duration") val videoDuration: Double,
        @JsonProperty("is_video") val isVideo: Boolean,
        @JsonProperty("has_audio") val hasAudio: Boolean,
        @JsonProperty("__typename") val typename: String,
        @JsonProperty("product_type") val productType: String,
        val dimensions: Dimensions,
    )

    data class Dimensions(val height: Int, val width: Int)

    override fun toMedia(
        filePath: Path,
        sourceUrl: String,
    ) = Media(
        type = MediaType.VIDEO,
        fileUrl = filePath.toAbsolutePath().toString(),
        sourceUrl = sourceUrl,
        title = this.node.title,
    )
}

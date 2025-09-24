package com.download.downloaderbot.infra.process.tools.instaloader

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class InstaloaderMedia(
    val node: Node
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Node(
        val title: String,
        val shortcode: String,
        @JsonProperty("video_duration") val videoDuration: Double,
        @JsonProperty("is_video") val isVideo: Boolean,
        @JsonProperty("has_audio") val hasAudio: Boolean,
        @JsonProperty("__typename") val typename: String,
        @JsonProperty("product_type") val productType: String,
        val dimensions: Dimensions
    )

    data class Dimensions(val height: Int, val width: Int)
}
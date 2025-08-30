package com.download.downloaderbot.core.tools.gallerydl

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class GalleryDlMedia(
    val filename: String,
    val extension: String,
    @JsonProperty("url")
    val sourceUrl: String,
    val path: String,
    @JsonAlias("title", "desc")
    val title: String,
    @JsonProperty("date")
    val publicationDate: String,
    val num: Int,
    val width: Int,
    val height: Int
)

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.ARRAY)
data class DumpLine3(
    val kind: Int,
    val url: String,
    val data: GalleryDlMedia
)

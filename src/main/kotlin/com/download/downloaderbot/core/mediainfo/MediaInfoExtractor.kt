package com.download.downloaderbot.core.mediainfo

import com.download.downloaderbot.core.entity.Media

interface MediaInfoExtractor {
    suspend fun fetchMediaInfo(url: String, path: String): Media
}
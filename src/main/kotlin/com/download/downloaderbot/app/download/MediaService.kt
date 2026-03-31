package com.download.downloaderbot.app.download

import com.download.downloaderbot.core.domain.Media

interface MediaService {
    suspend fun supports(url: String): Boolean
    suspend fun download(url: String): List<Media>
}

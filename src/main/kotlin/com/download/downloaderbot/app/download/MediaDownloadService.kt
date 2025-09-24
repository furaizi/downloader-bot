package com.download.downloaderbot.app.download

import com.download.downloaderbot.core.domain.Media

interface MediaDownloadService {
    suspend fun download(url: String): List<Media>
}

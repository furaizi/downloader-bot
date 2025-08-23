package com.download.downloaderbot.core.usecase

import com.download.downloaderbot.core.domain.Media

interface MediaDownloadService {
    suspend fun download(url: String): Media
}
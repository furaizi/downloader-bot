package com.download.downloaderbot.core.usecase

import com.download.downloaderbot.core.entity.Media

interface MediaDownloadService {
    suspend fun download(url: String): Media
}
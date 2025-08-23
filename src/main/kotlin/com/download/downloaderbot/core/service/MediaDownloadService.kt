package com.download.downloaderbot.core.service

import com.download.downloaderbot.core.domain.Media

interface MediaDownloadService {
    suspend fun download(url: String): Media
}
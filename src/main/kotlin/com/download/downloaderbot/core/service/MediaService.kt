package com.download.downloaderbot.core.service

import com.download.downloaderbot.core.entity.Media

interface MediaService {
    suspend fun download(url: String): Media
}
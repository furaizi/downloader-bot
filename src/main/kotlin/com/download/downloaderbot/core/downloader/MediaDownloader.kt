package com.download.downloaderbot.core.downloader

import com.download.downloaderbot.core.domain.Media

interface MediaDownloader {
//    fun supports(url: String): Boolean
    suspend fun download(url: String): List<Media>
}
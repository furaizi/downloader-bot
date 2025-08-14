package com.download.downloaderbot.core.downloader

interface MediaDownloader {
    suspend fun download(url: String, outputPath: String)
}
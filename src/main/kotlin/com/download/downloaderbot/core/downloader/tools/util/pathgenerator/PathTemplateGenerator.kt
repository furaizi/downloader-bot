package com.download.downloaderbot.core.downloader.tools.util.pathgenerator

interface PathTemplateGenerator {
    fun generate(url: String): DownloadPath
}
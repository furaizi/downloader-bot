package com.download.downloaderbot.core.tools.util

interface PathTemplateGenerator {
    fun generate(url: String): DownloadPath
}
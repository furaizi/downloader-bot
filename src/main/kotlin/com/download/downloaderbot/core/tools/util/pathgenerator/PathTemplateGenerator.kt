package com.download.downloaderbot.core.tools.util.pathgenerator

interface PathTemplateGenerator {
    fun generate(url: String): DownloadPath
}
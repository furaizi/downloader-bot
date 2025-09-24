package com.download.downloaderbot.infra.process.tools.util.pathgenerator

interface PathTemplateGenerator {
    fun generate(url: String): DownloadPath
}
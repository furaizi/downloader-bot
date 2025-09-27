package com.download.downloaderbot.infra.providers.interfaces

interface PathGenerator {
    fun generate(url: String): DownloadPath
}
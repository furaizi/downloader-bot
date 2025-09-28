package com.download.downloaderbot.infra.media.path

interface PathGenerator {
    fun generate(url: String): DownloadPath
}
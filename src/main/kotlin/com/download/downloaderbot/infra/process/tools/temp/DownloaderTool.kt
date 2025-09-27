package com.download.downloaderbot.infra.process.tools.temp

interface DownloaderTool<META> {
    suspend fun download(url: String, output: String)
    suspend fun probe(url: String, output: String? = null): META
}
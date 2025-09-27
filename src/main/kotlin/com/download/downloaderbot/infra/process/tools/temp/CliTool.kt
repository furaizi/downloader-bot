package com.download.downloaderbot.infra.process.tools.temp

interface CliTool<META> {
    suspend fun download(url: String, output: String)
    suspend fun probe(url: String, output: String? = null): META
}
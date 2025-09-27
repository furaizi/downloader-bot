package com.download.downloaderbot.infra.process.tools.temp

interface CommandBuilder {
    fun buildDownloadCommand(url: String, output: String): List<String>
    fun buildProbeCommand(url: String, output: String? = null): List<String>
}
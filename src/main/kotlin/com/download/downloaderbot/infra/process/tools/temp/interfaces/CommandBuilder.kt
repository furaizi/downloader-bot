package com.download.downloaderbot.infra.process.tools.temp.interfaces

interface CommandBuilder {
    fun downloadCommand(url: String, output: String): List<String>
    fun probeCommand(url: String, output: String? = null): List<String>
}
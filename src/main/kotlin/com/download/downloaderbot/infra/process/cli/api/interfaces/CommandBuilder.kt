package com.download.downloaderbot.infra.process.cli.api.interfaces

interface CommandBuilder {
    fun downloadCommand(url: String, output: String): List<String>
    fun probeCommand(url: String, output: String? = null): List<String>
}
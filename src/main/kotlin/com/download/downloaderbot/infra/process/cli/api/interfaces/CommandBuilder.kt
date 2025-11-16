package com.download.downloaderbot.infra.process.cli.api.interfaces

interface CommandBuilder {
    fun downloadCommand(
        url: String,
        output: String,
        formatOverride: String = "",
    ): List<String>

    fun probeCommand(
        url: String,
        output: String? = null,
        formatOverride: String = "",
    ): List<String>
}

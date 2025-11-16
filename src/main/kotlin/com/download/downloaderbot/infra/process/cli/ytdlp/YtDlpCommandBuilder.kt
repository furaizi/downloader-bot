package com.download.downloaderbot.infra.process.cli.ytdlp

import com.download.downloaderbot.app.config.properties.YtDlpProperties
import com.download.downloaderbot.infra.process.cli.api.interfaces.CommandBuilder

private val JSON_ONLY_ARGS =
    listOf(
        "--dump-json",
        "--no-warnings",
        "--skip-download",
    )

class YtDlpCommandBuilder(
    val props: YtDlpProperties,
) : CommandBuilder {
    override fun downloadCommand(
        url: String,
        output: String,
    ) = buildList {
        add(props.bin)
        addAll(buildConfigArgs())
        addAll(buildOutputArgs(output))
        add(url)
    }

    override fun probeCommand(
        url: String,
        output: String?,
    ) = buildList {
        add(props.bin)
        addAll(buildConfigArgs())
        addAll(JSON_ONLY_ARGS)
        add(url)
    }

    private fun buildConfigArgs(): List<String> =
        if (props.configFile.isNotBlank()) {
            listOf("--config-locations", props.configFile)
        } else {
            emptyList()
        }

    private fun buildOutputArgs(output: String) = listOf("-o", output)
}

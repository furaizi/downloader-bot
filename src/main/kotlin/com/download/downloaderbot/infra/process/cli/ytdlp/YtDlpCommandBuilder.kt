package com.download.downloaderbot.infra.process.cli.ytdlp

import com.download.downloaderbot.app.config.properties.YtDlpProperties
import com.download.downloaderbot.infra.process.cli.api.interfaces.CommandBuilder
import org.springframework.stereotype.Component

private val JSON_ONLY_ARGS = listOf(
    "--dump-json",
    "--no-warnings",
    "--skip-download"
)

class YtDlpCommandBuilder(
    val props: YtDlpProperties
) : CommandBuilder {

    override fun downloadCommand(url: String, output: String) = buildList {
        add(props.bin)
        addAll(buildOutputArgs(output))
        addAll(buildFormatArgs())
        addAll(props.extraArgs)
        add(url)
    }

    override fun probeCommand(url: String, output: String?) = buildList {
        add(props.bin)
        addAll(JSON_ONLY_ARGS)
        add(url)
    }

    private fun buildFormatArgs() = if (props.format.isNotBlank())
            listOf("-f", props.format)
        else
            emptyList()

    private fun buildOutputArgs(output: String) = listOf("-o", output)

}
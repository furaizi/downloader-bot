package com.download.downloaderbot.infra.process.cli.gallerydl

import com.download.downloaderbot.app.config.properties.GalleryDlProperties
import com.download.downloaderbot.infra.process.cli.api.interfaces.CommandBuilder
import org.springframework.stereotype.Component

class GalleryDlCommandBuilder(
    val props: GalleryDlProperties
) : CommandBuilder {

    override fun downloadCommand(url: String, output: String) = buildList {
        add(props.bin)
        addAll(buildOutputDirectoryArgs(output))
        addAll(buildOutputFilenameArgs())
        addAll(buildFilterArgs())
        addAll(props.extraArgs)
        add(url)
    }

    override fun probeCommand(url: String, output: String?) = emptyList<String>()

    private fun buildOutputDirectoryArgs(outputDirectory: String) = listOf("-D", outputDirectory)
    private fun buildOutputFilenameArgs() = listOf("-f", "{num}.{extension}")
    private fun buildFilterArgs() = listOf("--filter", "type == 'image'")
}
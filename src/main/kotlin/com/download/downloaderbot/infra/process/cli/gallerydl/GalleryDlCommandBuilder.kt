package com.download.downloaderbot.infra.process.cli.gallerydl

import com.download.downloaderbot.app.config.properties.GalleryDlProperties
import com.download.downloaderbot.infra.process.cli.api.interfaces.CommandBuilder

class GalleryDlCommandBuilder(
    val props: GalleryDlProperties,
) : CommandBuilder {
    override fun downloadCommand(
        url: String,
        output: String,
    ) = buildList {
        add(props.bin)
        addAll(buildConfigArgs())
        addAll(buildOutputDirectoryArgs(output))
        add(url)
    }

    override fun probeCommand(
        url: String,
        output: String?,
    ) = emptyList<String>()

    private fun buildConfigArgs(): List<String> =
        if (props.configFile.isNotBlank()) {
            listOf("--config", props.configFile)
        } else {
            emptyList()
        }

    private fun buildOutputDirectoryArgs(outputDirectory: String) = listOf("-D", outputDirectory)
}

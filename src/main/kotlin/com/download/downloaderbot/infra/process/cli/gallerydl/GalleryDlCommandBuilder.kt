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
        addAll(buildCookiesArgs())
        addAll(buildUserAgentArgs())
        addAll(buildOutputDirectoryArgs(output))
        addAll(buildOutputFilenameArgs())
        addAll(buildFilterArgs())
        addAll(props.extraArgs)
        add(url)
    }

    override fun probeCommand(
        url: String,
        output: String?,
    ) = emptyList<String>()

    private fun buildOutputDirectoryArgs(outputDirectory: String) = listOf("-D", outputDirectory)

    private fun buildOutputFilenameArgs() = listOf("-f", "{num}.{extension}")

    private fun buildFilterArgs() =
        listOf(
            "--filter",
            "extension in ['jpg','jpeg','png','webp','gif','mp4','mov','webm','heic']",
        )

    private fun buildCookiesArgs() =
        if (props.cookiesFile.isNotBlank()) {
            listOf("--cookies", props.cookiesFile)
        } else {
            emptyList()
        }

    private fun buildUserAgentArgs() =
        if (props.userAgent.isNotBlank()) {
            listOf("--user-agent", props.userAgent)
        } else {
            emptyList()
        }
}

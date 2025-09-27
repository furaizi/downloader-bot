package com.download.downloaderbot.infra.process.tools.temp.builder

import com.download.downloaderbot.app.config.properties.InstaloaderProperties
import com.download.downloaderbot.infra.process.tools.temp.interfaces.CommandBuilder
import com.download.downloaderbot.infra.process.tools.temp.builder.InstagramUtils.instagramShortcode
import okio.Path.Companion.toPath

private val VIDEO_ONLY_ARGS = listOf(
    "--no-pictures",
    "--no-captions",
    "--no-compress-json",
    "--no-metadata-json"
)

private val JSON_ONLY_ARGS = listOf(
    "--no-pictures",
    "--no-videos",
    "--no-video-thumbnails",
    "--no-captions",
    "--no-compress-json"
)

class InstaloaderCommandBuilder(
    val props: InstaloaderProperties
) : CommandBuilder {

    override fun downloadCommand(url: String, output: String) = buildList {
        val path = output.toPath()
        add(props.bin)
        addAll(buildSessionFileArgs())
        addAll(buildUserAgentArgs())
        addAll(VIDEO_ONLY_ARGS)
        addAll(buildOutputDirectoryArgs(path.parent.toString()))
        addAll(buildOutputFilenameArgs(path.name))
        addAll(props.extraArgs)
        addAll(buildUrlArgs(url))
    }

    override fun probeCommand(url: String, output: String?) = buildList {
        require(output != null) { "Output path is required for probing" }
        val path = output.toPath()
        add(props.bin)
        addAll(buildSessionFileArgs())
        addAll(buildUserAgentArgs())
        addAll(JSON_ONLY_ARGS)
        addAll(buildOutputDirectoryArgs(path.parent.toString()))
        addAll(buildOutputFilenameArgs(path.name))
        addAll(props.extraArgs)
        addAll(buildUrlArgs(url))

    }

    private fun buildSessionFileArgs() = if (props.sessionFile.isNotBlank())
            listOf("--sessionfile", props.sessionFile)
        else
            emptyList()

    private fun buildUserAgentArgs() = if (props.userAgent.isNotBlank())
            listOf("--user-agent", props.userAgent)
        else
            emptyList()

    private fun buildOutputDirectoryArgs(outputDirectory: String) = listOf("--dirname-pattern", outputDirectory)
    private fun buildOutputFilenameArgs(outputFilename: String) = listOf("--filename-pattern", outputFilename)
    private fun buildUrlArgs(url: String) = listOf("--", "-${url.instagramShortcode()}")

}
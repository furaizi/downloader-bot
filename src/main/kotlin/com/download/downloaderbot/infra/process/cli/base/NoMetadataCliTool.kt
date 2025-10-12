package com.download.downloaderbot.infra.process.cli.base

import com.download.downloaderbot.app.config.properties.MediaProperties
import com.download.downloaderbot.core.domain.Media
import com.download.downloaderbot.infra.media.files.FilesByPrefixFinder
import com.download.downloaderbot.infra.media.path.PathGenerator
import com.download.downloaderbot.infra.process.cli.api.CliTool
import com.download.downloaderbot.infra.process.cli.api.MediaConvertible
import com.download.downloaderbot.infra.process.cli.api.ToolId
import com.download.downloaderbot.infra.process.cli.api.interfaces.CommandBuilder
import com.download.downloaderbot.infra.process.cli.common.placeholder.EmptyPhotoMedia
import com.download.downloaderbot.infra.process.cli.common.placeholder.EmptyVideoMedia
import com.download.downloaderbot.infra.process.runner.ProcessRunner
import mu.KotlinLogging

private val log = KotlinLogging.logger {}

class NoMetadataCliTool(
    val props: MediaProperties,
    val pathGenerator: PathGenerator,
    val cmdBuilder: CommandBuilder,
    val runner: ProcessRunner,
    val fileFinder: FilesByPrefixFinder,
    override val toolId: ToolId,
) : CliTool {
    private val toolName = toolId.label

    override suspend fun download(
        url: String
    ): List<Media> {
        log.info { "Starting download: tool=$toolName, url=$url" }

        val (basePrefix, outputPath) = pathGenerator.generate(url)
        log.debug { "Generated path prefix=$basePrefix, outputPath=$outputPath" }

        val metaData = when (toolId) {
            ToolId.YT_DLP, ToolId.INSTALOADER -> EmptyVideoMedia()
            ToolId.GALLERY_DL -> EmptyPhotoMedia()
        }

        val cmd = cmdBuilder.downloadCommand(url, outputPath)
        log.debug { "Running download command: ${cmd.joinToString(" ")}" }

        runner.run(cmd, url)

        return resolveDownloadedMedia(basePrefix, url, metaData)

    }

    private suspend fun resolveDownloadedMedia(
        basePrefix: String,
        sourceUrl: String,
        metaData: MediaConvertible,
    ): List<Media> {
        val files = fileFinder.find(basePrefix, props.basePath)
        when (files.size) {
            1 -> log.info { "Download finished: $sourceUrl -> ${files.first()}" }
            else ->
                files.forEachIndexed { i, path ->
                    log.info { "Download finished [${i + 1}/${files.size}]: $sourceUrl -> $path" }
                }
        }
        return files.map { path -> metaData.toMedia(path, sourceUrl) }
    }

}

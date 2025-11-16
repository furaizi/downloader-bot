package com.download.downloaderbot.infra.process.cli.base

import com.download.downloaderbot.app.config.properties.MediaProperties
import com.download.downloaderbot.core.domain.Media
import com.download.downloaderbot.core.domain.MediaType
import com.download.downloaderbot.infra.media.files.FilesByPrefixFinder
import com.download.downloaderbot.infra.media.path.PathGenerator
import com.download.downloaderbot.infra.process.cli.api.CliTool
import com.download.downloaderbot.infra.process.cli.api.ToolId
import com.download.downloaderbot.infra.process.cli.api.interfaces.CommandBuilder
import com.download.downloaderbot.infra.process.runner.ProcessRunner
import mu.KotlinLogging
import java.nio.file.Path

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
        url: String,
        formatOverride: String,
    ): List<Media> {
        log.info { "Starting download: tool=$toolName, url=$url" }

        val (basePrefix, outputPath) = pathGenerator.generate(url)
        log.debug { "Generated path prefix=$basePrefix, outputPath=$outputPath" }

        val cmd = cmdBuilder.downloadCommand(url, outputPath, formatOverride)
        log.debug { "Running download command: ${cmd.joinToString(" ")}" }

        runner.run(cmd, url)

        return resolveDownloadedMedia(basePrefix, url)
    }

    private suspend fun resolveDownloadedMedia(
        basePrefix: String,
        sourceUrl: String,
    ): List<Media> {
        val files = fileFinder.find(basePrefix, props.basePath)

        when (files.size) {
            1 -> log.info { "Download finished: $sourceUrl -> ${files.first()}" }
            else ->
                files.forEachIndexed { i, path ->
                    log.info { "Download finished [${i + 1}/${files.size}]: $sourceUrl -> $path" }
                }
        }

        return files.map { path ->
            val mediaType = inferMediaType(path)
            Media(
                type = mediaType,
                fileUrl = path.toAbsolutePath().toString(),
                sourceUrl = sourceUrl,
                title = "",
            )
        }
    }

    private fun inferMediaType(path: Path): MediaType {
        val ext =
            path.fileName.toString()
                .substringAfterLast('.', "")
                .lowercase()

        return when (ext) {
            "jpg", "jpeg", "png", "webp", "gif", "bmp" -> MediaType.IMAGE
            "mp4", "m4v", "mov", "webm", "mkv", "avi" -> MediaType.VIDEO
            else ->
                when (toolId) {
                    ToolId.YT_DLP, ToolId.INSTALOADER -> MediaType.VIDEO
                    ToolId.GALLERY_DL -> MediaType.IMAGE
                }
        }
    }
}

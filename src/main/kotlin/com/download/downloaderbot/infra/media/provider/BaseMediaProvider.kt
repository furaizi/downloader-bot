package com.download.downloaderbot.infra.media.provider

import com.download.downloaderbot.app.config.properties.MediaProperties
import com.download.downloaderbot.core.domain.Media
import com.download.downloaderbot.core.downloader.MediaProvider
import com.download.downloaderbot.infra.media.files.FilesByPrefixFinder
import com.download.downloaderbot.infra.media.path.PathGenerator
import com.download.downloaderbot.infra.media.validation.ProbeValidator
import com.download.downloaderbot.infra.process.cli.api.CliTool
import com.download.downloaderbot.infra.process.cli.api.MediaConvertible
import com.download.downloaderbot.infra.process.cli.api.ToolId
import com.download.downloaderbot.infra.process.cli.common.placeholder.EmptyPhotoMedia
import com.download.downloaderbot.infra.process.cli.common.placeholder.EmptyVideoMedia
import mu.KotlinLogging
import java.nio.file.Path

private val log = KotlinLogging.logger {}

class BaseMediaProvider(
    val props: MediaProperties,
    val tool: CliTool<MediaConvertible>,
    val pathGenerator: PathGenerator,
    val fileFinder: FilesByPrefixFinder,
    val validators: List<ProbeValidator<MediaConvertible>> = emptyList(),
    val urlPredicate: (String) -> Boolean,
) : MediaProvider {
    val toolName = tool.toolId.label

    override fun supports(url: String): Boolean =
        urlPredicate(url)
            .also { log.debug { "Checking support for URL=$url with tool=$toolName: $it" } }

    override suspend fun download(url: String): List<Media> {
        log.info { "Starting download: tool=$toolName, url=$url" }

        val (basePrefix, outputPath) = pathGenerator.generate(url)
        log.debug { "Generated path prefix=$basePrefix, outputPath=$outputPath" }

        val metaData =
            runCatching { tool.probe(url, outputPath) }
                .onSuccess { log.debug { "Probe succeeded for url=$url" } }
                .onFailure { log.warn { "Probe failed for url=$url, using EmptyMedia" } }
                .getOrDefault(
                    when (tool.toolId) {
                        ToolId.YT_DLP, ToolId.INSTALOADER -> EmptyVideoMedia()
                        ToolId.GALLERY_DL -> EmptyPhotoMedia()
                    },
                )

        validators.forEach { validator ->
            log.debug { "Running validator=${validator::class.simpleName} for url=$url" }
            validator.validate(url, metaData)
        }

        tool.download(url, outputPath)
        return resolveDownloadedMedia(basePrefix, url, metaData)
    }

    private suspend fun resolveDownloadedMedia(
        basePrefix: String,
        sourceUrl: String,
        metaData: MediaConvertible,
    ): List<Media> {
        val files = fileFinder.find(basePrefix, props.basePath)
        logDownloads(sourceUrl, files)
        return files.map { path -> metaData.toMedia(path, sourceUrl) }
    }

    private fun logDownloads(
        url: String,
        files: List<Path>,
    ) {
        when (files.size) {
            1 -> log.info { "Download finished: $url -> ${files.first()}" }
            else ->
                files.forEachIndexed { i, path ->
                    log.info { "Download finished [${i + 1}/${files.size}]: $url -> $path" }
                }
        }
    }
}

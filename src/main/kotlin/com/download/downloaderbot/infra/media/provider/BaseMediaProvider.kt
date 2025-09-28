package com.download.downloaderbot.infra.media.provider

import com.download.downloaderbot.app.config.properties.MediaProperties
import com.download.downloaderbot.core.domain.Media
import com.download.downloaderbot.core.downloader.MediaProvider
import com.download.downloaderbot.infra.process.cli.api.CliTool
import com.download.downloaderbot.infra.process.cli.api.MediaConvertible
import com.download.downloaderbot.infra.media.files.FilesByPrefixFinder
import com.download.downloaderbot.infra.media.path.PathGenerator
import com.download.downloaderbot.infra.media.validation.ProbeValidator
import mu.KotlinLogging

private val log = KotlinLogging.logger {}

class BaseMediaProvider(
    val props: MediaProperties,
    val tool: CliTool<MediaConvertible>,
    val pathGenerator: PathGenerator,
    val fileFinder: FilesByPrefixFinder,
    val validators: List<ProbeValidator<MediaConvertible>> = emptyList(),
    val urlPredicate: (String) -> Boolean
) : MediaProvider {

    override fun supports(url: String): Boolean = urlPredicate(url)

    override suspend fun download(url: String): List<Media> {
        val (basePrefix, outputPath) = pathGenerator.generate(url)
        val metaData = tool.probe(url, outputPath)
        validators.forEach { it.validate(url, metaData) }
        tool.download(url, outputPath)
        return resolveDownloadedMedia(basePrefix, url, metaData)
    }

    private suspend fun resolveDownloadedMedia(
        basePrefix: String, sourceUrl: String, metaData: MediaConvertible
    ): List<Media> =
        fileFinder.find(basePrefix, props.basePath)
            .onEach { path -> log.info("download finished: $sourceUrl -> $path") }
            .map { path -> metaData.toMedia(path, sourceUrl) }
}
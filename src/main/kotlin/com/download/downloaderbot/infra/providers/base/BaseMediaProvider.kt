package com.download.downloaderbot.infra.providers.base

import com.download.downloaderbot.app.config.properties.MediaProperties
import com.download.downloaderbot.core.domain.Media
import com.download.downloaderbot.core.downloader.MediaProvider
import com.download.downloaderbot.infra.process.cli.api.CliTool
import com.download.downloaderbot.infra.providers.interfaces.FilesByPrefixFinder
import com.download.downloaderbot.infra.providers.interfaces.PathGenerator

class BaseMediaProvider(
    val props: MediaProperties,
    val tool: CliTool<in Any>,
    val pathGenerator: PathGenerator,
    val fileFinder: FilesByPrefixFinder
) : MediaProvider {


    override fun supports(url: String): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun download(url: String): List<Media> {
        TODO("Not yet implemented")
    }
}
package com.download.downloaderbot.infra.process.cli.common.extractor

import com.download.downloaderbot.core.downloader.MediaDownloaderToolException
import com.download.downloaderbot.infra.process.cli.api.interfaces.JsonExtractor

class OutputJsonExtractor(
    private val toolName: String
) : JsonExtractor {
    override suspend fun extract(source: String): String =
        source.lineSequence()
            .map { it.trim() }
            .firstOrNull { it.startsWith("{") && it.endsWith("}") }
            ?: throw MediaDownloaderToolException("$toolName produced no JSON", output = source)
}
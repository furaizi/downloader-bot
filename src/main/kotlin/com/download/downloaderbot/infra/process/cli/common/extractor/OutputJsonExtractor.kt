package com.download.downloaderbot.infra.process.cli.common.extractor

import com.download.downloaderbot.core.downloader.MediaDownloaderToolException
import com.download.downloaderbot.infra.process.cli.api.interfaces.JsonExtractor
import mu.KotlinLogging
import org.slf4j.MDC

private val log = KotlinLogging.logger {}

class OutputJsonExtractor(
    private val toolName: String
) : JsonExtractor {
    override suspend fun extract(source: String): String {
        MDC.put("tool", toolName)
        return try {
            log.debug { "Starting JSON extraction from output (length=${source.length})" }
            source.lineSequence()
                .map { it.trim() }
                .onEach { log.trace { "Checking line: $it" } }
                .firstOrNull { it.startsWith("{") && it.endsWith("}") }
                ?.also { log.debug { "JSON extracted successfully" } }
                ?: throw MediaDownloaderToolException("$toolName produced no JSON", output = source)
        } finally {
            MDC.remove("tool")
        }
    }
}
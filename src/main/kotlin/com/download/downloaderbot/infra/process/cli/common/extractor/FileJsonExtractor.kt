package com.download.downloaderbot.infra.process.cli.common.extractor

import com.download.downloaderbot.core.downloader.MediaDownloaderToolException
import com.download.downloaderbot.infra.process.cli.api.interfaces.JsonExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

class FileJsonExtractor(
    val toolName: String
) : JsonExtractor {

    override suspend fun extract(source: String): String = withContext(Dispatchers.IO) {
        val path = if (source.endsWith(".json")) source
                    else "$source.json"
        val file = File(path)

        if (!file.exists())
            throw MediaDownloaderToolException("$toolName produced no JSON", output = "File $path does not exist")

        try {
            file.readText()
        } catch (e: IOException) {
            throw MediaDownloaderToolException("Failed to read JSON produced by $toolName", output = e.message!!)
        } finally {
            runCatching { file.delete() }
        }
    }
}
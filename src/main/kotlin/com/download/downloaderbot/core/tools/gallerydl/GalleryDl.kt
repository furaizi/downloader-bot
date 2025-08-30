package com.download.downloaderbot.core.tools.gallerydl

import com.download.downloaderbot.core.config.properties.GalleryDlProperties
import com.download.downloaderbot.core.downloader.MediaDownloadException
import com.download.downloaderbot.core.tools.AbstractCliTool
import com.download.downloaderbot.core.tools.ForGalleryDl
import com.download.downloaderbot.core.tools.util.filefinder.FilesByPrefixFinder
import com.download.downloaderbot.core.tools.util.pathgenerator.PathTemplateGenerator
import com.fasterxml.jackson.databind.ObjectMapper
import mu.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.util.unit.DataSize
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.io.path.name
import kotlin.streams.asSequence
import kotlin.text.lineSequence

private val log = KotlinLogging.logger {}

@Service
class GalleryDl(
    val config: GalleryDlProperties,
    val mapper: ObjectMapper,
) : AbstractCliTool(config.bin) {

    suspend fun download(url: String, outputPath: String, maxSize: DataSize) {
        val args = listOf("-D", outputPath,
            "-f", "{num}.{extension}",
            "--filesize-max", maxSize.toGalleryDlArgs(),
            "--filter", "type == 'image'") +
            config.extraArgs
        execute(url, args)
    }

    suspend fun probe(url: String): GalleryDlMedia {
        val tmp = Files.createTempDirectory("gallery-dl-")
        val args = listOf("--no-download", "--write-info-json",
            "-D", tmp.toString())
        execute(url, args)
        val infoFile = Files.walk(tmp).use { stream ->
            stream.asSequence()
                .filter { Files.isRegularFile(it) && it.name == "info.json" }
                .first()
        }

    }

    private fun getJson(raw: String): String =
        raw.lineSequence()
            .map { it.trim() }
            .firstOrNull { it.startsWith("{") && it.endsWith("}") }
            ?: throw MediaDownloadException("gallery-dl produced no JSON", output = raw)

    private fun mapJsonToInnerMedia(json: String, url: String): GalleryDlMedia = try {
        mapper.readValue(json, GalleryDlMedia::class.java)
    } catch (e: Exception) {
        log.error(e) { "Failed to parse gallery-dl json for url=$url" }
        throw RuntimeException("Failed to parse gallery-dl output", e)
    }

    private fun parseProbe(raw: String, url: String): GalleryDlMedia {
        runCatching {
            val single = mapper.readValue(raw, GalleryDlMedia::class.java)
            if (single != null) {
                return single
            }
        }

        val root = mapper.readTree(raw)
        if (!root.isArray) {
            throw MediaDownloadException("gallery-dl returned unexpected JSON (not an array/object)", output = raw)
        }

        for (node in root) {
            if (node.isArray && node.size() >= 3 && node[0].asInt() == 3) {
                val line = mapper.treeToValue(node, DumpLine3::class.java)
                val media = line.data

                return media
            }
        }

        throw MediaDownloadException("gallery-dl produced no media items [3,...]", output = raw)
    }

    private fun DataSize.toGalleryDlArgs() = "${toMegabytes()}M"
}
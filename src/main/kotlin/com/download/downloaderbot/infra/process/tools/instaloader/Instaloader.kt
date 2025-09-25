package com.download.downloaderbot.infra.process.tools.instaloader

import com.download.downloaderbot.app.config.properties.InstaloaderProperties
import com.download.downloaderbot.infra.process.tools.AbstractCliTool
import com.download.downloaderbot.infra.process.tools.ytdlp.YtDlpMedia
import com.fasterxml.jackson.databind.ObjectMapper
import okio.Path.Companion.toPath
import org.springframework.stereotype.Service
import java.io.File

@Service
class Instaloader(
    val config: InstaloaderProperties,
    val mapper: ObjectMapper
) : AbstractCliTool(config.bin, config.timeout) {


    suspend fun download(url: String, outputPath: String) {
        val path = outputPath.toPath()
        val args = listOf("--no-pictures", "--no-captions", "--no-compress-json",
            "--dirname-pattern", path.parent.toString(),
            "--filename-pattern", path.name,
            "--", "-${extractShortcode(url)}") +
            config.extraArgs
        execute("", args)
    }

    suspend fun probe(url: String, outputPath: String): InstaloaderMedia {
        val path = outputPath.toPath()
        val args = listOf("--no-pictures", "--no-videos", "--no-video-thumbnails",
            "--no-captions", "--no-compress-json",
            "--dirname-pattern", path.parent.toString(),
            "--filename-pattern", path.name,
            "--", "-${extractShortcode(url)}")

        execute("", args)
        val json = readJson(outputPath)
        return mapJsonToInnerMedia(json, url)
    }

    private fun extractShortcode(url: String): String {
        val regex = Regex("""/(?:reel|p|tv)/([A-Za-z0-9_-]{5,})""")
        return regex.find(url)?.groupValues?.get(1) ?:
            throw IllegalArgumentException("Invalid Instagram URL: $url")
    }

    private suspend fun readJson(outputPath: String): String =
        File("$outputPath.json").readText()

    private fun mapJsonToInnerMedia(json: String, url: String): InstaloaderMedia = try {
        mapper.readValue(json, InstaloaderMedia::class.java)
    } catch (e: Exception) {
        throw RuntimeException("Failed to parse instaloader output for url=$url", e)
    }
}
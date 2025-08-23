package com.download.downloaderbot.core.ytdlp

import com.download.downloaderbot.core.config.properties.YtDlpProperties
import com.download.downloaderbot.core.downloader.MediaDownloadException
import com.download.downloaderbot.core.domain.Media
import com.download.downloaderbot.core.domain.MediaType
import com.download.downloaderbot.core.domain.YoutubeDlMedia
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import java.net.URI
import java.nio.file.Paths
import java.time.Instant
import java.util.UUID

private val log = KotlinLogging.logger {}

class NewYtDlp(
    val config: YtDlpProperties,
    val mapper: ObjectMapper
) {

    private val downloadsDir = Paths.get(config.baseDir)

    suspend fun download(url: String): Media {
        val outputPathTemplate = generatePath(url)
        val ytDlpMedia = probe(url)
        val media = Media(
            type = MediaType.fromString(ytDlpMedia.type),
            fileUrl = ytDlpMedia.,
            sourceUrl = url,
            title = ytDlpMedia.title
        )

        val args = listOf("-f", config.format, "-o", outputPathTemplate) + config.extraArgs
        execute(url, args)
        log.info { "yt-dlp download finished: $url -> $outputPath" }
    }

    private fun generatePath(url: String): String {
        val basePrefix = generateBasePrefix(url)
        return generateFilePathTemplate(basePrefix)
    }

    private suspend fun probe(url: String): YoutubeDlMedia {
        val json = dumpJson(url)
        return mapJsonToInnerMedia(json, url)
    }



    private fun generateBasePrefix(url: String): String {
        val host = runCatching { URI(url).host?.replace(":", "-") }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }
            ?: "media"
        val timestamp = Instant.now().toEpochMilli()
        val shortUuid = UUID.randomUUID().toString().take(8)
        return "$host-$timestamp-$shortUuid"
    }

    private fun generateFilePathTemplate(basePrefix: String): String =
        downloadsDir.resolve("$basePrefix.%(ext)s").toString()

    private suspend fun dumpJson(url: String): String {
        val args = listOf("--dump-json", "--no-warnings", "--skip-download")
        val raw = execute(url, args)
        return raw.lineSequence()
            .map { it.trim() }
            .firstOrNull { it.startsWith("{") && it.endsWith("}") }
            ?: throw MediaDownloadException("yt-dlp produced no JSON", exitCode = 0, output = raw)
    }

    private fun mapJsonToInnerMedia(json: String, url: String): YoutubeDlMedia = try {
        mapper.readValue(json, YoutubeDlMedia::class.java)
    } catch (e: Exception) {
        log.error(e) { "Failed to parse yt-dlp json for url=$url" }
        throw RuntimeException("Failed to parse yt-dlp output", e)
    }

    private suspend fun execute(url: String, args: List<String>) = coroutineScope {
        val cmd = buildCommand(url, args)
        val process = startProcess(cmd)
        val (readerJob, output) = startReaderJob(process)
        awaitProcessCompletion(process, readerJob, url, output)
        output.toString()
    }

    private suspend fun startProcess(cmd: List<String>) = withContext(Dispatchers.IO) {
        ProcessBuilder(cmd)
            .redirectErrorStream(true)
            .start()
    }

    private fun CoroutineScope.startReaderJob(process: Process): Pair<Job, StringBuilder> {
        val output = StringBuilder()
        val readerJob = launch(Dispatchers.IO) {
            process.inputStream.bufferedReader().useLines { lines ->
                lines.forEach { line ->
                    log.debug { line }
                    output.appendLine(line)
                }
            }
        }
        return readerJob to output
    }

    private suspend fun awaitProcessCompletion(
        process: Process,
        readerJob: Job,
        url: String,
        output: StringBuilder
    ) = try {
        val exitCode = awaitExitCode(process)
        readerJob.join()
        handleExitCode(exitCode, output.toString())
    } catch (ce: CancellationException) {
        log.info { "Cancelling download process for $url" }
        if (process.isAlive) process.destroyForcibly()
        throw ce
    } finally {
        runCatching { process.inputStream.close() }
        runCatching { process.errorStream.close() }
        runCatching { process.outputStream.close() }
        readerJob.cancel()
    }

    private suspend fun awaitExitCode(process: Process) = try {
        process.onExit().await().exitValue()
    } catch (t: Throwable) {
        if (process.isAlive) process.destroyForcibly()
        throw t
    }

    private fun handleExitCode(exitCode: Int, output: String) {
        if (exitCode != 0) {
            log.error { "yt-dlp failed (code=$exitCode). Output:\n$output" }
            throw MediaDownloadException(
                message = "yt-dlp failed with exit code $exitCode",
                exitCode = exitCode,
                output = output
            )
        }
    }

    private fun buildCommand(url: String, args: List<String> = emptyList()): List<String> {
        val command = mutableListOf(config.bin)
        command.addAll(args)
        command.add(url)
        return command
    }
}
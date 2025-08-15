package com.download.downloaderbot.core.downloader

import com.download.downloaderbot.core.config.YtDlpConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import org.springframework.stereotype.Component
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private val log = KotlinLogging.logger {}

@Component
class YoutubeDlMediaDownloader(
    val config: YtDlpConfig
) : MediaDownloader {

    override suspend fun download(url: String, outputPath: String) = coroutineScope {
        require(url.isNotBlank()) { "url must not be blank" }
        require(outputPath.isNotBlank()) { "outputPath must not be blank" }

        val cmd = buildCommand(url, outputPath)
        log.info { "Starting download: $url -> $outputPath\n$ ${cmd.joinToString(" ")}" }

        val process = withContext(Dispatchers.IO) {
            ProcessBuilder(cmd)
                .redirectErrorStream(true)
                .start()
        }

        val output = StringBuilder()

        val readerJob = launch(Dispatchers.IO) {
            process.inputStream.bufferedReader().useLines { lines ->
                lines.forEach { line ->
                    log.debug { line }
                    output.appendLine(line)
                }
            }
        }

        try {
            val exitCode = try {
                process.onExit().await().exitValue()
            } catch (t: Throwable) {
                if (process.isAlive) process.destroyForcibly()
                throw t
            }

            readerJob.join()

            if (exitCode != 0) {
                val out = output.toString()
                log.error { "yt-dlp failed (code=$exitCode). Output:\n$out" }
                throw MediaDownloadException(
                    message = "yt-dlp failed with exit code $exitCode",
                    exitCode = exitCode,
                    output = out
                )
            }

            log.info { "Download finished: $outputPath" }
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
    }

    private fun buildCommand(url: String, outputPath: String): List<String> {
        return listOf(
            config.bin,
            "-f", config.format,
            "-o", outputPath
        ) + config.extraArgs + url
    }
}
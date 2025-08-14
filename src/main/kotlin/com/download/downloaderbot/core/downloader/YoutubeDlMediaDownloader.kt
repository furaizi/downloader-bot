package com.download.downloaderbot.core.downloader

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
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
class YoutubeDlMediaDownloader : MediaDownloader {

    override suspend fun download(url: String, outputPath: String) = coroutineScope {
        val processBuilder = ProcessBuilder(
            "yt-dlp",
            "-f", "best",
            "-o", outputPath,
            url
        )
            .redirectErrorStream(true)

        val process = withContext(Dispatchers.IO) {
            processBuilder.start()
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

        log.info { "Starting download: $url -> $outputPath" }

        val exitCode = suspendCancellableCoroutine<Int> { cont ->
            val future = process.onExit()
            future.whenComplete { proc, throwable ->
                if (throwable != null) cont.resumeWithException(throwable)
                else cont.resume(proc.exitValue())
            }
            cont.invokeOnCancellation {
                try { future.cancel(true) } catch (_: Exception) {}
                if (process.isAlive) {
                    log.info { "Cancelling process for $url" }
                    process.destroyForcibly()
                }
            }
        }

        try {
            readerJob.join()
        } catch (_: CancellationException) {}

        if (exitCode != 0) {
            val out = output.toString()
            log.error { "yt-dlp failed (code=$exitCode). Output:\n$out" }
            throw RuntimeException("yt-dlp failed with exit code $exitCode")
        }

        log.info { "Download finished: $outputPath" }
    }
}
package com.download.downloaderbot.core.tools.ytdlp

import com.download.downloaderbot.core.downloader.MediaDownloadException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mu.KotlinLogging

private val log = KotlinLogging.logger {}

abstract class AbstractCliTool(
    val bin: String
) {

    protected suspend fun execute(url: String, args: List<String>) = coroutineScope {
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
            log.error { "$bin failed (code=$exitCode). Output:\n$output" }
            throw MediaDownloadException(
                message = "$bin failed with exit code $exitCode",
                exitCode = exitCode,
                output = output
            )
        }
    }

    private fun buildCommand(url: String, args: List<String> = emptyList()): List<String> {
        val command = mutableListOf(bin)
        command.addAll(args)
        command.add(url)
        return command
    }
}
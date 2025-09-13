package com.download.downloaderbot.core.downloader.tools

import com.download.downloaderbot.core.downloader.ToolExecutionException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withContext
import mu.KotlinLogging

private val log = KotlinLogging.logger {}

abstract class AbstractCliTool(
    private val bin: String
) {

    protected suspend fun execute(url: String, args: List<String> = emptyList()): String = coroutineScope {
        val cmd = buildCommand(url, args)
        val process = startProcess(cmd)
        val outputDeferred = collectProcessOutputAsync(process)

        try {
            val exitCode = process.awaitExitCode()
            val output = outputDeferred.await()
            handleExitCode(exitCode, output)
            output
        } catch (ce: CancellationException) {
            log.info { "Cancelling download process for $url" }
            if (process.isAlive)
                process.destroyForcibly()
            throw ce
        } finally {
            withContext(NonCancellable) {
                process.closeStreams()
                stopTasks(process, outputDeferred)
            }
        }
    }

    private fun buildCommand(url: String, args: List<String>): List<String> =
        buildList {
            add(bin)
            addAll(args)
            add(url)
        }

    private suspend fun startProcess(cmd: List<String>) = withContext(Dispatchers.IO) {
        ProcessBuilder(cmd)
            .redirectErrorStream(true)
            .start()
    }

    private fun CoroutineScope.collectProcessOutputAsync(process: Process) =
        async(Dispatchers.IO, start = CoroutineStart.UNDISPATCHED) {
        process.inputStream.bufferedReader(Charsets.UTF_8).useLines { lines ->
            buildString {
                lines.forEach { line ->
                    log.debug { line }
                    appendLine(line)
                }
            }
        }
    }

    private suspend fun Process.awaitExitCode(): Int = try {
        onExit()
            .await()
            .exitValue()
    } catch (t: Throwable) {
        if (isAlive)
            destroyForcibly()
        throw t
    }

    private fun handleExitCode(exitCode: Int, output: String) {
        if (exitCode != 0)
            throw ToolExecutionException(bin, exitCode, output)
    }

    private fun Process.closeStreams() {
        runCatching { inputStream.close() }
        runCatching { errorStream.close() }
        runCatching { outputStream.close() }
    }

    private suspend fun <T> stopTasks(process: Process, deferred: Deferred<T>) {
        runCatching { deferred.cancelAndJoin() }
        runCatching { process.onExit().await() }
    }
}

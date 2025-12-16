package com.download.downloaderbot.infra.process.runner

import com.download.downloaderbot.core.downloader.ToolExecutionException
import com.download.downloaderbot.core.downloader.ToolTimeoutException
import com.download.downloaderbot.infra.process.cli.common.utils.human
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import mu.KotlinLogging
import java.time.Duration
import kotlin.time.TimeSource

private val log = KotlinLogging.logger {}
private val clock = TimeSource.Monotonic

class DefaultProcessRunner(
    private val bin: String,
    private val timeout: Duration,
) : ProcessRunner {
    private data class OutputCollection(
        val deferred: Deferred<String>,
        val buffer: StringBuffer,
    ) {
        fun snapshot(): String = buffer.toString()
    }

    override suspend fun run(
        args: List<String>,
        url: String,
    ): String =
        coroutineScope {
            val mark = clock.markNow()
            val timeoutMs = timeout.toMillis().coerceAtLeast(0)
            log.info { "Starting process: bin=$bin, cmd=$args, timeout=${timeoutMs}ms" }

            val process = startProcess(args)
            val pidInfo = runCatching { process.pid() }.getOrNull()?.let { "(pid=$it)" } ?: ""
            log.debug { "Process started $pidInfo" }

            val output = collectProcessOutputAsync(process)

            try {
                val exitCode =
                    if (timeoutMs > 0) {
                        withTimeout(timeoutMs) { process.awaitExitCode() }
                    } else {
                        process.awaitExitCode()
                    }

                val fullOutput = output.deferred.await()
                handleExitCode(exitCode, fullOutput)

                val dur = mark.elapsedNow()
                log.info { "Process $pidInfo finished successfully: exitCode=$exitCode, duration=${dur.human()}" }
                log.debug { "Process $pidInfo durationMs=${dur.inWholeMilliseconds}" }

                fullOutput
            } catch (te: TimeoutCancellationException) {
                log.warn { "Timeout waiting for $bin (url=$url, timeout=${timeoutMs}ms). Killing process tree..." }
                process.killTree()

                val partial = output.snapshot()

                throw ToolTimeoutException(bin, timeout, partial, te)
            } catch (ce: CancellationException) {
                log.info { "Cancelling download process for $url" }
                process.killTree()
                throw ce
            } finally {
                withContext(NonCancellable) {
                    process.closeStreams()
                    stopTasks(process, output.deferred)
                }
            }
        }

    private suspend fun startProcess(args: List<String>) =
        withContext(Dispatchers.IO) {
            ProcessBuilder(args)
                .redirectErrorStream(true)
                .start()
        }

    private fun CoroutineScope.collectProcessOutputAsync(process: Process): OutputCollection {
        val buffer = StringBuffer()

        val deferred =
            async(Dispatchers.IO) {
                process.inputStream.bufferedReader(Charsets.UTF_8).useLines { lines ->
                    lines.forEach { line ->
                        log.debug { line }
                        buffer.append(line).append('\n')
                    }
                }
                buffer.toString()
            }

        return OutputCollection(deferred, buffer)
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun Process.awaitExitCode(): Int =
        try {
            onExit().await().exitValue()
        } catch (t: Throwable) {
            if (isAlive) destroyForcibly()
            throw t
        }

    private fun handleExitCode(
        exitCode: Int,
        output: String,
    ) {
        if (exitCode != 0) throw ToolExecutionException(bin, exitCode, output)
    }

    private fun Process.closeStreams() {
        runCatching { inputStream.close() }
        runCatching { errorStream.close() }
        runCatching { outputStream.close() }
    }

    private suspend fun <T> stopTasks(
        process: Process,
        deferred: Deferred<T>,
    ) {
        runCatching { deferred.cancelAndJoin() }
        runCatching { process.onExit().await() }
    }

    private fun Process.killTree() {
        runCatching {
            val root = toHandle()

            root.descendants().use { stream ->
                stream.forEach { runCatching { it.destroy() } }
            }
            runCatching { destroy() }

            root.descendants().use { stream ->
                stream.forEach { runCatching { it.destroyForcibly() } }
            }
            if (isAlive) destroyForcibly()
        }.onFailure {
            log.warn(it) { "Failed to kill process tree for $bin" }
            runCatching { if (isAlive) destroyForcibly() }
        }
    }
}

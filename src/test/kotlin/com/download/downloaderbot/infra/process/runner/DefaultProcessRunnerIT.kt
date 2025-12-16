package com.download.downloaderbot.infra.process.runner

import com.download.downloaderbot.core.downloader.ToolExecutionException
import com.download.downloaderbot.core.downloader.ToolTimeoutException
import com.download.downloaderbot.infra.process.utils.PosixShellCondition
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.annotation.EnabledIf
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.longs.shouldBeLessThan
import io.kotest.matchers.longs.shouldBeGreaterThanOrEqual
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlinx.coroutines.withTimeout
import java.time.Duration
import kotlin.time.Duration.Companion.seconds

@EnabledIf(PosixShellCondition::class)
class DefaultProcessRunnerIT : FunSpec({

    fun createRunner(timeout: Duration = Duration.ofSeconds(5)) = DefaultProcessRunner("sh", timeout)

    fun shCommand(script: String): List<String> = listOf("/bin/sh", "-c", script)

    test("should successfully execute command and return output") {
        val runner = createRunner()
        val result = runner.run(shCommand("echo 'Hello, World!'"), "url")
        result.trim() shouldBe "Hello, World!"
    }

    test("should throw ToolExecutionException containing output when exit code is not 0") {
        val runner = createRunner()
        val errorMsg = "Critical failure"
        val script = "echo '$errorMsg' >&2; exit 1"

        val exception =
            shouldThrow<ToolExecutionException> {
                runner.run(shCommand(script), "url")
            }

        assertSoftly(exception) {
            exitCode shouldBe 1
            tool shouldBe "sh"
            output shouldContain errorMsg
        }
    }

    test("should throw ToolTimeoutException when process exceeds timeout") {
        val runnerTimeout = Duration.ofMillis(200)
        val runner = createRunner(runnerTimeout)
        val script = "sleep 1; echo 'too late'"

        val exception =
            shouldThrow<ToolTimeoutException> {
                runner.run(shCommand(script), "url")
            }

        assertSoftly(exception) {
            tool shouldBe "sh"
            timeout shouldBe runnerTimeout
        }
    }

    test("should handle partial output before timeout") {
        val runner = createRunner(Duration.ofMillis(500))
        val script = "echo 'Started...'; sleep 2"

        val exception =
            shouldThrow<ToolTimeoutException> {
                runner.run(shCommand(script), "url")
            }

        exception.output shouldContain "Started..."
    }

    test("handles multi-megabyte output") {
        val runner = createRunner(Duration.ofSeconds(10))
        val targetBytes = 10 * 1024 * 1024 // 10 MB
        val script = "yes 0123456789abcdef | head -c $targetBytes"

        val output = withTimeout(15.seconds) {
            runner.run(shCommand(script), "big-output-url")
        }

        assertSoftly(output.length.toLong()) {
            shouldBeGreaterThanOrEqual(targetBytes - 32L)
            shouldBeLessThan(targetBytes + 64L)
        }
    }
})

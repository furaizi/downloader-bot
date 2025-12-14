package com.download.downloaderbot.infra.process.cli.common

import com.download.downloaderbot.infra.process.cli.api.interfaces.CommandBuilder

class ShellYtDlpCommandBuilder : CommandBuilder {
    private var probeScript: String = "exit 2"
    private var downloadExts: List<String> = emptyList()

    fun probeOk(jsonLine: String) = apply {
        probeScript = "printf '%s\\n' ${shQuote("noise")}; printf '%s\\n' ${shQuote(jsonLine)}"
    }

    fun downloadCreatesFiles(exts: List<String>) = apply { downloadExts = exts }

    override fun probeCommand(url: String, output: String?, formatOverride: String): List<String> =
        sh(probeScript)

    override fun downloadCommand(url: String, output: String, formatOverride: String): List<String> {
        val touches =
            downloadExts.joinToString("; ") { ext ->
                val file = output.replace("%(ext)s", ext)
                ": > ${shQuote(file)}"
            }.ifBlank { ":" }

        return sh("$touches; :")
    }
}

class ShellGalleryDlCommandBuilder : CommandBuilder {
    private var probeScript: String = "exit 2"
    private var fileNames: List<String> = emptyList()

    fun probeFails(exitCode: Int = 2) = apply {
        probeScript = "echo ${shQuote("probe failed")} 1>&2; exit $exitCode"
    }

    fun downloadCreatesDirWithFiles(fileNames: List<String>) = apply { this.fileNames = fileNames }

    override fun probeCommand(url: String, output: String?, formatOverride: String): List<String> =
        sh(probeScript)

    override fun downloadCommand(url: String, output: String, formatOverride: String): List<String> {
        val mkdir = "mkdir -p ${shQuote(output)}"
        val touches =
            fileNames.joinToString("; ") { name ->
                ": > ${shQuote("$output/$name")}"
            }
        return sh(listOf(mkdir, touches, ":").filter { it.isNotBlank() }.joinToString("; "))
    }
}

private fun sh(script: String): List<String> = listOf("/bin/sh", "-c", script)

private fun shQuote(value: String): String = "'" + value.replace("'", "'\"'\"'") + "'"

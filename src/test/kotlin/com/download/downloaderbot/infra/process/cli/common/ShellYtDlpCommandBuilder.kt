package com.download.downloaderbot.infra.process.cli.common

import com.download.downloaderbot.infra.process.cli.api.interfaces.CommandBuilder

abstract class AbstractFakeShellCommandBuilder<SELF : AbstractFakeShellCommandBuilder<SELF>> : CommandBuilder {
    protected var probeScript: String = Scripts.EXIT_ERROR

    @Suppress("UNCHECKED_CAST")
    protected val self: SELF
        get() = this as SELF

    fun probeFails(exitCode: Int = 2): SELF {
        probeScript = "echo ${"probe failed".quoted()} 1>&2; exit $exitCode"
        return self
    }

    override fun probeCommand(
        url: String,
        output: String?,
        formatOverride: String,
    ): List<String> = wrapInShell(probeScript)

    protected fun wrapInShell(script: String): List<String> = listOf("/bin/sh", "-c", script)
}

class ShellYtDlpCommandBuilder : AbstractFakeShellCommandBuilder<ShellYtDlpCommandBuilder>() {
    private var downloadScriptGenerator: (outputTemplate: String) -> String = { Scripts.NO_OP }

    fun probeOk(jsonLine: String) =
        apply {
            probeScript = "printf '%s\\n' ${jsonLine.quoted()}"
        }

    fun downloadCreatesFiles(exts: List<String>) =
        apply {
            downloadScriptGenerator = { outputTemplate ->
                val commands =
                    exts.map { ext ->
                        val filename = outputTemplate.replace("%(ext)s", ext)
                        Scripts.touch(filename)
                    }
                commands.joinToString("; ")
            }
        }

    override fun downloadCommand(
        url: String,
        output: String,
        formatOverride: String,
    ): List<String> {
        val script = downloadScriptGenerator(output)
        return wrapInShell(script)
    }
}

class ShellGalleryDlCommandBuilder : AbstractFakeShellCommandBuilder<ShellGalleryDlCommandBuilder>() {
    private var downloadScriptGenerator: (outputDir: String) -> String = { Scripts.NO_OP }

    fun downloadCreatesDirWithFiles(fileNames: List<String>) =
        apply {
            downloadScriptGenerator = { outputDir ->
                buildList {
                    add(Scripts.mkdir(outputDir))
                    addAll(fileNames.map { fileName -> Scripts.touch("$outputDir/$fileName") })
                }.joinToString("; ")
            }
        }

    override fun downloadCommand(
        url: String,
        output: String,
        formatOverride: String,
    ): List<String> {
        val script = downloadScriptGenerator(output)
        return wrapInShell(script)
    }
}

private object Scripts {
    const val NO_OP = ":"
    const val EXIT_ERROR = "exit 2"

    fun touch(path: String): String = ": > ${path.quoted()}"

    fun mkdir(path: String): String = "mkdir -p ${path.quoted()}"
}

private fun String.quoted(): String {
    return "'" + this.replace("'", "'\"'\"'") + "'"
}

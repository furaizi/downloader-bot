package com.download.downloaderbot.infra.process.cli.base

import com.download.downloaderbot.infra.process.cli.api.CliTool
import com.download.downloaderbot.infra.process.cli.api.ToolId
import com.download.downloaderbot.infra.process.cli.api.interfaces.CommandBuilder
import com.download.downloaderbot.infra.process.runner.ProcessRunner

class DownloadOnlyCliTool<T>(
    val runner: ProcessRunner,
    val cmdBuilder: CommandBuilder,
    override val toolId: ToolId
): CliTool<T> {

    override suspend fun download(url: String, output: String) {
        val cmd = cmdBuilder.downloadCommand(url, output)
        runner.run(cmd, url)
    }

    override suspend fun probe(url: String, output: String?): T =
        throw UnsupportedOperationException("${toolId.label}: probe is not supported yet")
}
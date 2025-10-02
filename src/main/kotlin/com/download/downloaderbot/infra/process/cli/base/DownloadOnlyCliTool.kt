package com.download.downloaderbot.infra.process.cli.base

import com.download.downloaderbot.infra.process.cli.api.CliTool
import com.download.downloaderbot.infra.process.cli.api.ToolId
import com.download.downloaderbot.infra.process.cli.api.interfaces.CommandBuilder
import com.download.downloaderbot.infra.process.runner.ProcessRunner
import mu.KotlinLogging
import org.slf4j.MDC

private val log = KotlinLogging.logger {}

class DownloadOnlyCliTool<T>(
    val runner: ProcessRunner,
    val cmdBuilder: CommandBuilder,
    override val toolId: ToolId,
) : CliTool<T> {
    override suspend fun download(
        url: String,
        output: String,
    ) {
        MDC.put("tool", toolId.label)
        try {
            val cmd = cmdBuilder.downloadCommand(url, output)
            log.debug { "Running download command: ${cmd.joinToString(" ")}" }
            runner.run(cmd, url)
        } finally {
            MDC.remove("tool")
        }
    }

    override suspend fun probe(
        url: String,
        output: String?,
    ): T = throw UnsupportedOperationException("${toolId.label}: probe is not supported yet")
}

package com.download.downloaderbot.infra.process.cli.base

import com.download.downloaderbot.infra.process.cli.api.CliTool
import com.download.downloaderbot.infra.process.cli.api.ToolId
import com.download.downloaderbot.infra.process.runner.ProcessRunner
import com.download.downloaderbot.infra.process.cli.api.interfaces.CommandBuilder
import com.download.downloaderbot.infra.process.cli.api.interfaces.JsonExtractor
import com.download.downloaderbot.infra.process.cli.api.interfaces.JsonParser
import com.download.downloaderbot.infra.process.cli.common.utils.preview
import mu.KotlinLogging
import org.slf4j.MDC

private val log = KotlinLogging.logger {}

class BaseCliTool<META>(
    val runner: ProcessRunner,
    val cmdBuilder: CommandBuilder,
    val jsonExtractor: JsonExtractor,
    val jsonParser: JsonParser<META>,
    override val toolId: ToolId
) : CliTool<META> {

    override suspend fun download(url: String, output: String) {
        MDC.put("tool", toolId.name)
        try {
            val cmd = cmdBuilder.downloadCommand(url, output)
            log.debug { "Running download command: ${cmd.joinToString(" ")}" }

            runner.run(cmd, url)
        } finally {
            MDC.remove("tool")
        }
    }

    override suspend fun probe(url: String, output: String?): META {
        MDC.put("tool", toolId.name)
        try {
            val cmd = cmdBuilder.probeCommand(url, output)
            log.debug { "Running probe command: ${cmd.joinToString(" ")}" }

            val processOutput = runner.run(cmd, url)
            log.trace { "Probe result (raw): $processOutput" }

            val json = jsonExtractor.extract(processOutput)
            log.trace { "Extracted JSON: ${json.preview()}" }

            return jsonParser.parse(json)
        } finally {
            MDC.remove("tool")
        }
    }
}
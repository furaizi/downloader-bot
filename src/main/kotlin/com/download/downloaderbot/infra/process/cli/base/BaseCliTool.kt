package com.download.downloaderbot.infra.process.cli.base

import com.download.downloaderbot.infra.process.cli.api.CliTool
import com.download.downloaderbot.infra.process.runner.ProcessRunner
import com.download.downloaderbot.infra.process.cli.api.interfaces.CommandBuilder
import com.download.downloaderbot.infra.process.cli.api.interfaces.JsonExtractor
import com.download.downloaderbot.infra.process.cli.api.interfaces.JsonParser

class BaseCliTool<META>(
    val exec: ProcessRunner,
    val cmdBuilder: CommandBuilder,
    val jsonExtractor: JsonExtractor,
    val jsonParser: JsonParser<META>
) : CliTool<META> {

    override suspend fun download(url: String, output: String) {
        val cmd = cmdBuilder.downloadCommand(url, output)
        exec.run(cmd, url)
    }

    override suspend fun probe(url: String, output: String?): META {
        val cmd = cmdBuilder.probeCommand(url, output)
        val result = exec.run(cmd, url)
        val json = jsonExtractor.extract(result)
        return jsonParser.parse(json)
    }
}
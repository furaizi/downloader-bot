package com.download.downloaderbot.infra.process.tools.temp

import com.download.downloaderbot.infra.process.tools.ProcessRunner
import com.download.downloaderbot.infra.process.tools.temp.interfaces.CommandBuilder
import com.download.downloaderbot.infra.process.tools.temp.interfaces.JsonExtractor
import com.download.downloaderbot.infra.process.tools.temp.interfaces.JsonParser

abstract class AbstractCliTool<META>(
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
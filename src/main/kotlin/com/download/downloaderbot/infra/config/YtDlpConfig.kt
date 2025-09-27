package com.download.downloaderbot.infra.config

import com.download.downloaderbot.app.config.properties.YtDlpProperties
import com.download.downloaderbot.infra.process.cli.api.CliTool
import com.download.downloaderbot.infra.process.cli.api.ToolId
import com.download.downloaderbot.infra.process.cli.api.interfaces.CommandBuilder
import com.download.downloaderbot.infra.process.cli.api.interfaces.JsonExtractor
import com.download.downloaderbot.infra.process.cli.api.interfaces.JsonParser
import com.download.downloaderbot.infra.process.cli.base.BaseCliTool
import com.download.downloaderbot.infra.process.cli.common.extractor.OutputJsonExtractor
import com.download.downloaderbot.infra.process.cli.common.parser.DefaultJsonParser
import com.download.downloaderbot.infra.process.cli.ytdlp.YtDlpCommandBuilder
import com.download.downloaderbot.infra.process.cli.ytdlp.YtDlpMedia
import com.download.downloaderbot.infra.process.runner.DefaultProcessRunner
import com.download.downloaderbot.infra.process.runner.ProcessRunner
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class YtDlpConfig(val props: YtDlpProperties) {

    @Bean
    fun ytDlp(
        ytDlpRunner: ProcessRunner,
        ytDlpCommandBuilder: CommandBuilder,
        ytDlpExtractor: JsonExtractor,
        ytDlpParser: JsonParser<YtDlpMedia>
    ): CliTool<YtDlpMedia> =
        BaseCliTool(ytDlpRunner, ytDlpCommandBuilder,
            ytDlpExtractor, ytDlpParser, ToolId.YT_DLP)

    @Bean
    fun ytDlpRunner(): ProcessRunner =
        DefaultProcessRunner(props.bin, props.timeout)

    @Bean
    fun ytDlpCommandBuilder(): CommandBuilder =
        YtDlpCommandBuilder(props)

    @Bean
    fun ytDlpExtractor(): JsonExtractor =
        OutputJsonExtractor(props.bin)

    @Bean
    fun ytDlpParser(mapper: ObjectMapper): JsonParser<YtDlpMedia> =
        DefaultJsonParser(mapper, object : TypeReference<YtDlpMedia>() {})

}
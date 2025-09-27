package com.download.downloaderbot.infra.config

import com.download.downloaderbot.app.config.properties.InstaloaderProperties
import com.download.downloaderbot.app.config.properties.YtDlpProperties
import com.download.downloaderbot.infra.process.cli.api.CliTool
import com.download.downloaderbot.infra.process.cli.api.interfaces.CommandBuilder
import com.download.downloaderbot.infra.process.cli.api.interfaces.JsonExtractor
import com.download.downloaderbot.infra.process.cli.api.interfaces.JsonParser
import com.download.downloaderbot.infra.process.cli.base.BaseCliTool
import com.download.downloaderbot.infra.process.cli.common.extractor.FileJsonExtractor
import com.download.downloaderbot.infra.process.cli.common.extractor.OutputJsonExtractor
import com.download.downloaderbot.infra.process.cli.common.parser.DefaultJsonParser
import com.download.downloaderbot.infra.process.cli.instaloader.InstaloaderMedia
import com.download.downloaderbot.infra.process.cli.ytdlp.YtDlpCommandBuilder
import com.download.downloaderbot.infra.process.cli.ytdlp.YtDlpMedia
import com.download.downloaderbot.infra.process.runner.ProcessRunner
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class CliToolConfig {

    @Bean
    fun ytDlp(
        ytDlpRunner: ProcessRunner,
        ytDlpCommandBuilder: CommandBuilder,
        ytDlpJsonExtractor: JsonExtractor,
        ytDlpJsonParser: JsonParser<YtDlpMedia>
    ): CliTool<YtDlpMedia> =
        BaseCliTool(ytDlpRunner, ytDlpCommandBuilder, ytDlpJsonExtractor, ytDlpJsonParser)

    @Bean
    fun instaloader(
        instaloaderRunner: ProcessRunner,
        instaloaderCommandBuilder: CommandBuilder,
        instaloaderJsonExtractor: JsonExtractor,
        instaloaderJsonParser: JsonParser<InstaloaderMedia>
    ): CliTool<InstaloaderMedia> =
        BaseCliTool(instaloaderRunner, instaloaderCommandBuilder, instaloaderJsonExtractor, instaloaderJsonParser
    )


    @Bean
    fun ytDlpJsonExtractor(props: YtDlpProperties): JsonExtractor =
        OutputJsonExtractor(props.bin)

    @Bean
    fun instaloaderJsonExtractor(props: InstaloaderProperties): JsonExtractor =
        FileJsonExtractor(props.bin)

    @Bean
    fun ytDlpJsonParser(mapper: ObjectMapper): JsonParser<YtDlpMedia> =
        DefaultJsonParser(mapper, object : TypeReference<YtDlpMedia>() {})

    @Bean
    fun instaloaderJsonParser(mapper: ObjectMapper): JsonParser<InstaloaderMedia> =
        DefaultJsonParser(mapper, object : TypeReference<InstaloaderMedia>() {})
}
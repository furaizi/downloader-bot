package com.download.downloaderbot.infra.config.tools

import com.download.downloaderbot.app.config.properties.InstaloaderProperties
import com.download.downloaderbot.infra.config.jsonParser
import com.download.downloaderbot.infra.process.cli.api.CliTool
import com.download.downloaderbot.infra.process.cli.api.ToolId
import com.download.downloaderbot.infra.process.cli.api.interfaces.CommandBuilder
import com.download.downloaderbot.infra.process.cli.api.interfaces.JsonExtractor
import com.download.downloaderbot.infra.process.cli.api.interfaces.JsonParser
import com.download.downloaderbot.infra.process.cli.base.BaseCliTool
import com.download.downloaderbot.infra.process.cli.common.extractor.FileJsonExtractor
import com.download.downloaderbot.infra.process.cli.instaloader.InstaloaderCommandBuilder
import com.download.downloaderbot.infra.process.cli.instaloader.InstaloaderMedia
import com.download.downloaderbot.infra.process.runner.DefaultProcessRunner
import com.download.downloaderbot.infra.process.runner.ProcessRunner
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class InstaloaderConfig(val props: InstaloaderProperties) {

    @Bean
    fun instaloader(
        instaloaderRunner: ProcessRunner,
        instaloaderCommandBuilder: CommandBuilder,
        instaloaderJsonExtractor: JsonExtractor,
        instaloaderJsonParser: JsonParser<InstaloaderMedia>
    ): CliTool<InstaloaderMedia> =
        BaseCliTool(instaloaderRunner, instaloaderCommandBuilder,
            instaloaderJsonExtractor, instaloaderJsonParser, ToolId.INSTALOADER)

    @Bean
    fun instaloaderRunner(): ProcessRunner =
        DefaultProcessRunner(props.bin, props.timeout)

    @Bean
    fun instaloaderCommandBuilder(): CommandBuilder =
        InstaloaderCommandBuilder(props)

    @Bean
    fun instaloaderJsonExtractor(): JsonExtractor =
        FileJsonExtractor(props.bin)

    @Bean
    fun instaloaderJsonParser(mapper: ObjectMapper): JsonParser<InstaloaderMedia> =
        jsonParser(mapper)
}
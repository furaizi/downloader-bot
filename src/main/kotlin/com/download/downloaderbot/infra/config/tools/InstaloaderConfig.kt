package com.download.downloaderbot.infra.config.tools

import com.download.downloaderbot.app.config.properties.InstaloaderProperties
import com.download.downloaderbot.app.config.properties.MediaProperties
import com.download.downloaderbot.infra.config.createJsonParser
import com.download.downloaderbot.infra.di.ForInstaloader
import com.download.downloaderbot.infra.media.files.FilesByPrefixFinder
import com.download.downloaderbot.infra.media.path.InstaloaderPathGenerator
import com.download.downloaderbot.infra.media.path.PathGenerator
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
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@ConditionalOnProperty(prefix = "downloader.instaloader", name = ["enabled"], havingValue = "true")
class InstaloaderConfig(
    val mediaProps: MediaProperties,
    val toolProps: InstaloaderProperties
) {

    @Bean
    fun instaloader(
        pathGenerator: PathGenerator,
        commandBuilder: CommandBuilder,
        processRunner: ProcessRunner,
        jsonExtractor: JsonExtractor,
        jsonParser: JsonParser<InstaloaderMedia>,
        @ForInstaloader fileFinder: FilesByPrefixFinder
    ): CliTool =
        BaseCliTool(
            mediaProps,
            pathGenerator,
            commandBuilder,
            processRunner,
            jsonExtractor,
            jsonParser,
            fileFinder,
            ToolId.YT_DLP
        )

    @Bean
    fun pathGenerator(): PathGenerator = InstaloaderPathGenerator(mediaProps)

    @Bean
    fun commandBuilder(): CommandBuilder = InstaloaderCommandBuilder(toolProps)

    @Bean
    fun processRunner(): ProcessRunner = DefaultProcessRunner(toolProps.bin, toolProps.timeout)

    @Bean
    fun jsonExtractor(): JsonExtractor = FileJsonExtractor(toolProps.bin)

    @Bean
    fun jsonParser(mapper: ObjectMapper): JsonParser<InstaloaderMedia> = createJsonParser(mapper)
}

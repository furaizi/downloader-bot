package com.download.downloaderbot.infra.config.tools

import com.download.downloaderbot.app.config.properties.MediaProperties
import com.download.downloaderbot.app.config.properties.YtDlpProperties
import com.download.downloaderbot.infra.config.createJsonParser
import com.download.downloaderbot.infra.di.ForYtDlp
import com.download.downloaderbot.infra.media.files.FilesByPrefixFinder
import com.download.downloaderbot.infra.media.path.PathGenerator
import com.download.downloaderbot.infra.media.path.YtDlpPathGenerator
import com.download.downloaderbot.infra.process.cli.api.CliTool
import com.download.downloaderbot.infra.process.cli.api.ToolId
import com.download.downloaderbot.infra.process.cli.api.interfaces.CommandBuilder
import com.download.downloaderbot.infra.process.cli.api.interfaces.JsonExtractor
import com.download.downloaderbot.infra.process.cli.api.interfaces.JsonParser
import com.download.downloaderbot.infra.process.cli.base.BaseCliTool
import com.download.downloaderbot.infra.process.cli.common.extractor.OutputJsonExtractor
import com.download.downloaderbot.infra.process.cli.ytdlp.YtDlpCommandBuilder
import com.download.downloaderbot.infra.process.cli.ytdlp.YtDlpMedia
import com.download.downloaderbot.infra.process.runner.DefaultProcessRunner
import com.download.downloaderbot.infra.process.runner.ProcessRunner
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@ConditionalOnProperty(prefix = "downloader.yt-dlp", name = ["enabled"], havingValue = "true", matchIfMissing = true)
class YtDlpConfig(
    val mediaProps: MediaProperties,
    val toolProps: YtDlpProperties
) {

    @Bean
    fun ytDlp(
        pathGenerator: PathGenerator,
        commandBuilder: CommandBuilder,
        processRunner: ProcessRunner,
        jsonExtractor: JsonExtractor,
        jsonParser: JsonParser<YtDlpMedia>,
        @ForYtDlp fileFinder: FilesByPrefixFinder
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
    fun pathGenerator(): PathGenerator = YtDlpPathGenerator(mediaProps)

    @Bean
    fun commandBuilder(): CommandBuilder = YtDlpCommandBuilder(toolProps)

    @Bean
    fun processRunner(): ProcessRunner = DefaultProcessRunner(toolProps.bin, toolProps.timeout)

    @Bean
    fun jsonExtractor(): JsonExtractor = OutputJsonExtractor(toolProps.bin)

    @Bean
    fun jsonParser(mapper: ObjectMapper): JsonParser<YtDlpMedia> = createJsonParser(mapper)
}

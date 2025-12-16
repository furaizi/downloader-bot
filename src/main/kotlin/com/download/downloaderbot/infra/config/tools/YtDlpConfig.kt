package com.download.downloaderbot.infra.config.tools

import com.download.downloaderbot.app.config.properties.MediaProperties
import com.download.downloaderbot.app.config.properties.YtDlpProperties
import com.download.downloaderbot.infra.di.ForYtDlp
import com.download.downloaderbot.infra.media.files.FilesByPrefixFinder
import com.download.downloaderbot.infra.media.path.YtDlpPathGenerator
import com.download.downloaderbot.infra.process.cli.api.CliTool
import com.download.downloaderbot.infra.process.cli.api.ToolId
import com.download.downloaderbot.infra.process.cli.base.BaseCliTool
import com.download.downloaderbot.infra.process.cli.common.extractor.OutputJsonExtractor
import com.download.downloaderbot.infra.process.cli.common.parser.DefaultJsonParser
import com.download.downloaderbot.infra.process.cli.ytdlp.YtDlpCommandBuilder
import com.download.downloaderbot.infra.process.cli.ytdlp.YtDlpMedia
import com.download.downloaderbot.infra.process.runner.DefaultProcessRunner
import com.download.downloaderbot.infra.process.runner.ProcessRunner
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@ConditionalOnProperty(prefix = "downloader.yt-dlp", name = ["enabled"], havingValue = "true", matchIfMissing = true)
class YtDlpConfig(
    val mediaProps: MediaProperties,
    val toolProps: YtDlpProperties,
) {
    @Bean
    @ForYtDlp
    fun ytDlpProcessRunner(): ProcessRunner = DefaultProcessRunner(toolProps.bin, toolProps.timeout)

    @Bean
    fun ytDlp(
        mapper: ObjectMapper,
        @ForYtDlp fileFinder: FilesByPrefixFinder,
        @ForYtDlp processRunner: ProcessRunner,
    ): CliTool =
        BaseCliTool(
            mediaProps,
            YtDlpPathGenerator(mediaProps),
            YtDlpCommandBuilder(toolProps),
            processRunner,
            OutputJsonExtractor(toolProps.bin),
            DefaultJsonParser(mapper, object : TypeReference<YtDlpMedia>() {}),
            fileFinder,
            ToolId.YT_DLP,
        )
}

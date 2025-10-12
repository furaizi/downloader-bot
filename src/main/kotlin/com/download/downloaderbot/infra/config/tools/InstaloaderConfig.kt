package com.download.downloaderbot.infra.config.tools

import com.download.downloaderbot.app.config.properties.InstaloaderProperties
import com.download.downloaderbot.app.config.properties.MediaProperties
import com.download.downloaderbot.infra.config.createJsonParser
import com.download.downloaderbot.infra.di.ForInstaloader
import com.download.downloaderbot.infra.media.files.FilesByPrefixFinder
import com.download.downloaderbot.infra.media.path.InstaloaderPathGenerator
import com.download.downloaderbot.infra.process.cli.api.CliTool
import com.download.downloaderbot.infra.process.cli.api.ToolId
import com.download.downloaderbot.infra.process.cli.base.BaseCliTool
import com.download.downloaderbot.infra.process.cli.common.extractor.FileJsonExtractor
import com.download.downloaderbot.infra.process.cli.instaloader.InstaloaderCommandBuilder
import com.download.downloaderbot.infra.process.runner.DefaultProcessRunner
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
        mapper: ObjectMapper,
        @ForInstaloader fileFinder: FilesByPrefixFinder
    ): CliTool =
        BaseCliTool(
            mediaProps,
            InstaloaderPathGenerator(mediaProps),
            InstaloaderCommandBuilder(toolProps),
            DefaultProcessRunner(toolProps.bin, toolProps.timeout),
            FileJsonExtractor(toolProps.bin),
            createJsonParser(mapper),
            fileFinder,
            ToolId.YT_DLP
        )

}

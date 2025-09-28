package com.download.downloaderbot.infra.config.tools

import com.download.downloaderbot.app.config.properties.GalleryDlProperties
import com.download.downloaderbot.infra.process.cli.api.CliTool
import com.download.downloaderbot.infra.process.cli.api.ToolId
import com.download.downloaderbot.infra.process.cli.api.interfaces.CommandBuilder
import com.download.downloaderbot.infra.process.cli.base.DownloadOnlyCliTool
import com.download.downloaderbot.infra.process.cli.gallerydl.GalleryDlCommandBuilder
import com.download.downloaderbot.infra.process.cli.gallerydl.GalleryDlMedia
import com.download.downloaderbot.infra.process.runner.DefaultProcessRunner
import com.download.downloaderbot.infra.process.runner.ProcessRunner
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@ConditionalOnProperty(prefix = "downloader.gallery-dl", name = ["enabled"], havingValue = "true")
class GalleryDlConfig(val props: GalleryDlProperties) {

    @Bean
    fun galleryDl(
        galleryDlRunner: ProcessRunner,
        galleryDlCommandBuilder: CommandBuilder,
    ): CliTool<GalleryDlMedia> =
        DownloadOnlyCliTool(galleryDlRunner, galleryDlCommandBuilder, ToolId.GALLERY_DL)

    @Bean
    fun galleryDlRunner(): ProcessRunner =
        DefaultProcessRunner(props.bin, props.timeout)

    @Bean
    fun galleryDlCommandBuilder(): CommandBuilder =
        GalleryDlCommandBuilder(props)

}
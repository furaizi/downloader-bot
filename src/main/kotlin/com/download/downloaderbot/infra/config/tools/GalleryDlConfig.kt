package com.download.downloaderbot.infra.config.tools

import com.download.downloaderbot.app.config.properties.GalleryDlProperties
import com.download.downloaderbot.app.config.properties.MediaProperties
import com.download.downloaderbot.infra.di.ForGalleryDl
import com.download.downloaderbot.infra.media.files.FilesByPrefixFinder
import com.download.downloaderbot.infra.media.path.GalleryDlPathGenerator
import com.download.downloaderbot.infra.process.cli.api.CliTool
import com.download.downloaderbot.infra.process.cli.api.ToolId
import com.download.downloaderbot.infra.process.cli.base.NoMetadataCliTool
import com.download.downloaderbot.infra.process.cli.gallerydl.GalleryDlCommandBuilder
import com.download.downloaderbot.infra.process.runner.DefaultProcessRunner
import com.download.downloaderbot.infra.process.runner.ProcessRunner
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@ConditionalOnProperty(prefix = "downloader.gallery-dl", name = ["enabled"], havingValue = "true")
class GalleryDlConfig(
    val mediaProps: MediaProperties,
    val toolProps: GalleryDlProperties,
) {
    @Bean
    @ForGalleryDl
    fun galleryDlProcessRunner(): ProcessRunner = DefaultProcessRunner(toolProps.bin, toolProps.timeout)

    @Bean
    fun galleryDl(
        @ForGalleryDl fileFinder: FilesByPrefixFinder,
        @ForGalleryDl processRunner: ProcessRunner,
    ): CliTool =
        NoMetadataCliTool(
            mediaProps,
            GalleryDlPathGenerator(mediaProps),
            GalleryDlCommandBuilder(toolProps),
            processRunner,
            fileFinder,
            ToolId.GALLERY_DL,
        )
}

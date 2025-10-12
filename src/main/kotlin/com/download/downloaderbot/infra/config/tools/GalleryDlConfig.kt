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
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@ConditionalOnProperty(prefix = "downloader.gallery-dl", name = ["enabled"], havingValue = "true")
class GalleryDlConfig(
    val mediaProps: MediaProperties,
    val toolProps: GalleryDlProperties
) {
    @Bean
    fun galleryDl(
        @ForGalleryDl fileFinder: FilesByPrefixFinder
    ): CliTool =
        NoMetadataCliTool(
            mediaProps,
            GalleryDlPathGenerator(mediaProps),
            GalleryDlCommandBuilder(toolProps),
            DefaultProcessRunner(toolProps.bin, toolProps.timeout),
            fileFinder,
            ToolId.GALLERY_DL
        )

}

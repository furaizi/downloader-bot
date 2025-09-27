package com.download.downloaderbot.infra.config

import com.download.downloaderbot.app.config.properties.MediaProperties
import com.download.downloaderbot.core.downloader.MediaProvider
import com.download.downloaderbot.infra.process.cli.api.CliTool
import com.download.downloaderbot.infra.process.cli.gallerydl.GalleryDlMedia
import com.download.downloaderbot.infra.process.cli.ytdlp.YtDlpMedia
import com.download.downloaderbot.infra.di.ForGalleryDl
import com.download.downloaderbot.infra.di.ForYtDlp
import com.download.downloaderbot.infra.providers.base.BaseMediaProvider
import com.download.downloaderbot.infra.providers.interfaces.FilesByPrefixFinder
import com.download.downloaderbot.infra.providers.interfaces.PathGenerator
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ProvidersConfig(
    val props: MediaProperties
) {

    @Bean
    fun tikTokVideoDownloader(
        ytDlp: CliTool<YtDlpMedia>,
        ytDlpPathGenerator: PathGenerator,
        @ForYtDlp filesByPrefixFinder: FilesByPrefixFinder
    ): MediaProvider =
        BaseMediaProvider(props, ytDlp, ytDlpPathGenerator, filesByPrefixFinder, urlPredicate = {
            it.containsAll("tiktok.com", "video")
        })

    @Bean
    fun tikTokPhotoDownloader(
        galleryDl: CliTool<GalleryDlMedia>,
        galleryDlPathGenerator: PathGenerator,
        @ForGalleryDl filesByPrefixFinder: FilesByPrefixFinder
    ): MediaProvider =
        BaseMediaProvider(props, galleryDl, galleryDlPathGenerator, filesByPrefixFinder, urlPredicate = {
            it.containsAll("tiktok.com", "photo")
        })


}
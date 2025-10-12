package com.download.downloaderbot.infra.config.providers

import com.download.downloaderbot.core.downloader.MediaProvider
import com.download.downloaderbot.infra.config.containsAll
import com.download.downloaderbot.infra.config.tools.GalleryDlConfig
import com.download.downloaderbot.infra.config.tools.YtDlpConfig
import com.download.downloaderbot.infra.media.provider.BaseMediaProvider
import com.download.downloaderbot.infra.process.cli.api.CliTool
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class TikTokProvidersConfig {
    @Bean
    @ConditionalOnBean(YtDlpConfig::class)
    fun tikTokVideoDownloader(ytDlp: CliTool): MediaProvider =
        BaseMediaProvider(ytDlp) { url ->
            url.containsAll("tiktok.com", "video")
        }

    @Bean
    @ConditionalOnBean(GalleryDlConfig::class)
    fun tikTokPhotoDownloader(galleryDl: CliTool, ): MediaProvider =
        BaseMediaProvider(galleryDl) { url ->
            url.containsAll("tiktok.com", "photo")
        }
}

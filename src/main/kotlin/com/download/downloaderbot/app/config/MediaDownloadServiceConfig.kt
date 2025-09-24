package com.download.downloaderbot.app.config

import com.download.downloaderbot.core.downloader.MediaDownloader
import com.download.downloaderbot.app.download.DefaultMediaDownloadService
import com.download.downloaderbot.app.download.MediaDownloadService
import com.download.downloaderbot.app.download.MediaInterceptor
import com.download.downloaderbot.app.download.PipelineMediaDownloadService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary

@Configuration
class MediaDownloadServiceConfig {

    @Bean
    fun mediaDownloadService(downloaders: List<MediaDownloader>): MediaDownloadService =
        DefaultMediaDownloadService(downloaders)

    @Primary
    @Bean
    fun pipelineMediaDownloadService(
        core: MediaDownloadService,
        interceptors: List<MediaInterceptor>
    ): MediaDownloadService =
        PipelineMediaDownloadService(core, interceptors)
}

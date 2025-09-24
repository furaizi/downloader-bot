package com.download.downloaderbot.core.config

import com.download.downloaderbot.core.downloader.MediaDownloader
import com.download.downloaderbot.core.service.DefaultMediaDownloadService
import com.download.downloaderbot.core.service.MediaDownloadService
import com.download.downloaderbot.core.service.MediaInterceptor
import com.download.downloaderbot.core.service.PipelineMediaDownloadService
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
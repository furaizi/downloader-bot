package com.download.downloaderbot.app.config

import com.download.downloaderbot.core.downloader.MediaProvider
import com.download.downloaderbot.app.download.DefaultMediaService
import com.download.downloaderbot.app.download.MediaService
import com.download.downloaderbot.app.download.MediaInterceptor
import com.download.downloaderbot.app.download.PipelineMediaService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary

@Configuration
class MediaDownloadServiceConfig {

    @Bean
    fun mediaDownloadService(downloaders: Map<String, MediaProvider>): MediaService =
        DefaultMediaService(downloaders)

    @Primary
    @Bean
    fun pipelineMediaDownloadService(
        core: MediaService,
        interceptors: List<MediaInterceptor>
    ): MediaService =
        PipelineMediaService(core, interceptors)
}

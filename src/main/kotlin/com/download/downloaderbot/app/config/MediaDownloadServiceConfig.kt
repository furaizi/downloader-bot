package com.download.downloaderbot.app.config

import com.download.downloaderbot.app.download.MediaInterceptor
import com.download.downloaderbot.app.download.MediaService
import com.download.downloaderbot.app.download.PipelineMediaService
import com.download.downloaderbot.app.download.SupportsInterceptor
import com.download.downloaderbot.core.downloader.MediaProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary

@Configuration
class MediaDownloadServiceConfig {

    @Primary
    @Bean
    fun pipelineMediaDownloadService(
        core: MediaProvider,
        supportsInterceptors: List<SupportsInterceptor>,
        mediaInterceptors: List<MediaInterceptor>,
    ): MediaService = PipelineMediaService(core, supportsInterceptors, mediaInterceptors)
}

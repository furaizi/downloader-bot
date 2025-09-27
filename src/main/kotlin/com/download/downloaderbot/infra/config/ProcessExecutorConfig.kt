package com.download.downloaderbot.infra.config

import com.download.downloaderbot.app.config.properties.GalleryDlProperties
import com.download.downloaderbot.app.config.properties.InstaloaderProperties
import com.download.downloaderbot.app.config.properties.YtDlpProperties
import com.download.downloaderbot.infra.process.tools.DefaultProcessExecutor
import com.download.downloaderbot.infra.process.tools.ProcessExecutor
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ProcessExecutorConfig {

    @Bean
    fun ytDlpExecutor(props: YtDlpProperties): ProcessExecutor =
        DefaultProcessExecutor(props.bin, props.timeout)

    @Bean
    fun instaloaderExecutor(props: InstaloaderProperties): ProcessExecutor =
        DefaultProcessExecutor(props.bin, props.timeout)

    @Bean
    fun galleryDlExecutor(props: GalleryDlProperties): ProcessExecutor =
        DefaultProcessExecutor(props.bin, props.timeout)
}
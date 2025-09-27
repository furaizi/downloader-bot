package com.download.downloaderbot.infra.config

import com.download.downloaderbot.app.config.properties.GalleryDlProperties
import com.download.downloaderbot.app.config.properties.InstaloaderProperties
import com.download.downloaderbot.app.config.properties.YtDlpProperties
import com.download.downloaderbot.infra.process.tools.DefaultProcessRunner
import com.download.downloaderbot.infra.process.tools.ProcessRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ProcessExecutorConfig {

    @Bean
    fun ytDlpExecutor(props: YtDlpProperties): ProcessRunner =
        DefaultProcessRunner(props.bin, props.timeout)

    @Bean
    fun instaloaderExecutor(props: InstaloaderProperties): ProcessRunner =
        DefaultProcessRunner(props.bin, props.timeout)

    @Bean
    fun galleryDlExecutor(props: GalleryDlProperties): ProcessRunner =
        DefaultProcessRunner(props.bin, props.timeout)
}
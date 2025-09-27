package com.download.downloaderbot.infra.config

import com.download.downloaderbot.app.config.properties.GalleryDlProperties
import com.download.downloaderbot.app.config.properties.InstaloaderProperties
import com.download.downloaderbot.app.config.properties.YtDlpProperties
import com.download.downloaderbot.infra.process.runner.DefaultProcessRunner
import com.download.downloaderbot.infra.process.runner.ProcessRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ProcessRunnerConfig {

    @Bean
    fun ytDlpRunner(props: YtDlpProperties): ProcessRunner =
        DefaultProcessRunner(props.bin, props.timeout)

    @Bean
    fun instaloaderRunner(props: InstaloaderProperties): ProcessRunner =
        DefaultProcessRunner(props.bin, props.timeout)

    @Bean
    fun galleryDlRunner(props: GalleryDlProperties): ProcessRunner =
        DefaultProcessRunner(props.bin, props.timeout)
}
package com.download.downloaderbot.bot.config

import com.download.downloaderbot.app.download.StubMediaService
import com.download.downloaderbot.bot.gateway.RecordingBotPort
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary

@TestConfiguration
class DownloadJobExecutorTestConfig {

    @Primary
    @Bean
    fun stubMediaService() = StubMediaService()

    @Primary
    @Bean
    fun recordingBotPort() = RecordingBotPort()

}
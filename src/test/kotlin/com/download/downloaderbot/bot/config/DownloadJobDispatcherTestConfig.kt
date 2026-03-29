package com.download.downloaderbot.bot.config

import com.download.downloaderbot.app.download.StubMediaService
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary

@TestConfiguration
class DownloadJobDispatcherTestConfig {
    @Primary
    @Bean
    fun stubMediaService() = StubMediaService()
}

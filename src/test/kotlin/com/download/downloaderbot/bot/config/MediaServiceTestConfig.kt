package com.download.downloaderbot.bot.config

import com.download.downloaderbot.core.downloader.MediaProvider
import com.download.downloaderbot.infra.network.StubFinalUrlResolver
import io.mockk.mockk
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary

@TestConfiguration
class MediaServiceTestConfig {
    @Bean
    @Primary
    fun mediaProviderMock(): MediaProvider = mockk(relaxed = true)

    @Primary
    @Bean
    fun stubFinalUrlResolver() = StubFinalUrlResolver()
}

package com.download.downloaderbot.bot.config

import com.download.downloaderbot.app.config.properties.CacheProperties
import com.download.downloaderbot.app.download.StubMediaService
import com.download.downloaderbot.bot.config.properties.BotIdentity
import com.download.downloaderbot.bot.config.properties.BotProperties
import com.download.downloaderbot.bot.gateway.RecordingBotPort
import com.download.downloaderbot.bot.job.DefaultDownloadJobExecutor
import com.download.downloaderbot.bot.promo.PromoService
import com.download.downloaderbot.infra.cache.InMemoryMediaCacheAdapter
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

    @Primary
    @Bean
    fun inMemoryCache() = InMemoryMediaCacheAdapter()

    @Primary
    @Bean
    fun testDownloadJobExecutor(
        mediaService: StubMediaService,
        botPort: RecordingBotPort,
        botProps: BotProperties,
        cachePort: InMemoryMediaCacheAdapter,
        cacheProps: CacheProperties,
        promoService: PromoService,
        botIdentity: BotIdentity,
    ) = DefaultDownloadJobExecutor(
        mediaService,
        botPort,
        botProps,
        cachePort,
        cacheProps,
        promoService,
        botIdentity,

    )

}
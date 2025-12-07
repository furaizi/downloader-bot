package com.download.downloaderbot.bot.job

import com.download.downloaderbot.app.config.properties.CacheProperties
import com.download.downloaderbot.app.download.MediaService
import com.download.downloaderbot.bot.config.properties.BotIdentity
import com.download.downloaderbot.bot.config.properties.BotProperties
import com.download.downloaderbot.bot.gateway.BotPort
import com.download.downloaderbot.bot.promo.PromoService
import com.download.downloaderbot.core.cache.CachePort
import com.download.downloaderbot.core.domain.Media
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean

@TestConfiguration
class DownloadJobExecutorTestConfig {

    @Bean
    fun botProperties() =
        BotProperties(
            token = "test-token",
            shareText = "Share this bot",
            promoText = "Promo text",
            promoEveryN = 1,
        )


    @Bean
    fun cacheProperties() = CacheProperties(schemaVersion = 1)

    @Bean
    fun downloadJobExecutor(
        mediaService: MediaService,
        botPort: BotPort,
        props: BotProperties,
        cachePort: CachePort<String, List<Media>>,
        cacheProps: CacheProperties,
        promoService: PromoService,
        botIdentity: BotIdentity,
    ) = DefaultDownloadJobExecutor(
        mediaService,
        botPort,
        props,
        cachePort,
        cacheProps,
        promoService,
        botIdentity,
    )

}
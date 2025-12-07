package com.download.downloaderbot.bot.config

import com.download.downloaderbot.app.config.properties.CacheProperties
import com.download.downloaderbot.bot.config.properties.BotProperties
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary

@TestConfiguration
class DownloadJobExecutorTestConfig {

    @Primary
    @Bean
    fun botProperties() =
        BotProperties(
            token = "test-token",
            shareText = "Share this bot",
            promoText = "Promo text",
            promoEveryN = 1,
        )


    @Primary
    @Bean
    fun cacheProperties() = CacheProperties(schemaVersion = 1)

}
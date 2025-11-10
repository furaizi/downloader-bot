package com.download.downloaderbot.bot.config

import com.download.downloaderbot.bot.config.properties.BotIdentity
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class BotIdentityConfig {

    @Bean
    fun botIdentity() = BotIdentity("<uninitialized>")

}
package com.download.downloaderbot.bot.config

import com.download.downloaderbot.bot.config.properties.BotProperties
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class StartupChecks {
    @Bean
    fun checkToken(props: BotProperties) =
        ApplicationRunner {
            require(props.token.isNotBlank()) { "Telegram bot token must be provided" }
        }
}

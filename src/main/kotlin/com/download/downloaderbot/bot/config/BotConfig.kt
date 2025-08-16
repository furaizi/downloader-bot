package com.download.downloaderbot.bot.config

import com.download.downloaderbot.bot.config.properties.BotProperties
import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import mu.KotlinLogging
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

private val log = KotlinLogging.logger {}

@Configuration
class BotConfig(val botProperties: BotProperties) {

    @Bean
    fun telegramBot(): Bot = bot {
        token = botProperties.token
        dispatch {

        }
    }
}
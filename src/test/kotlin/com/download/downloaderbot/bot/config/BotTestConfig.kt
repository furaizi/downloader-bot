package com.download.downloaderbot.bot.config

import com.download.downloaderbot.bot.config.properties.BotIdentity
import com.download.downloaderbot.bot.gateway.RecordingTelegramBot
import com.github.kotlintelegrambot.Bot
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary

@TestConfiguration
class BotTestConfig {
    @Primary
    @Bean
    fun testBotIdentity() = BotIdentity("test-bot")

    @Primary
    @Bean
    fun recordingTelegramBot() = RecordingTelegramBot(username = "test-bot")

    @Primary
    @Bean
    fun testBot(recordingTelegramBot: RecordingTelegramBot): Bot = recordingTelegramBot.bot
}

package com.download.downloaderbot.bot.config

import com.download.downloaderbot.bot.config.properties.BotIdentity
import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.User
import com.github.kotlintelegrambot.types.TelegramBotResult
import io.mockk.every
import io.mockk.mockk
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary

@TestConfiguration
class TestBotConfig {

    @Primary
    @Bean
    fun testBotIdentity() = BotIdentity("test-bot")

    @Primary
    @Bean
    fun testBot(): Bot = mockk(relaxed = true) {
        every { getMe() } returns TelegramBotResult.Success(User(id = 0L, isBot = true, firstName = "t", username = "test-bot"))
    }

}
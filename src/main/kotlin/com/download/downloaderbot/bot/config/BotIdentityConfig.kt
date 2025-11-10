package com.download.downloaderbot.bot.config

import com.download.downloaderbot.bot.config.properties.BotIdentity
import com.github.kotlintelegrambot.Bot
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class BotIdentityConfig {

    @Bean
    fun botIdentity(bot: Bot): BotIdentity {
        val botUser = bot.getMe()
            .getOrNull()
            ?: error("Failed to get bot info from Telegram API")

        val botUsername = (botUser.username ?: error("Bot has no username"))
            .removePrefix("@")

        return BotIdentity(botUsername)
    }

}
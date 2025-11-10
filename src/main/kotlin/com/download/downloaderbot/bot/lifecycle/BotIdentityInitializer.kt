package com.download.downloaderbot.bot.lifecycle

import com.download.downloaderbot.bot.config.properties.BotIdentity
import com.github.kotlintelegrambot.Bot
import org.springframework.beans.factory.SmartInitializingSingleton
import org.springframework.stereotype.Component

@Component
class BotIdentityInitializer(
    private val botIdentity: BotIdentity,
    private val bot: Bot,
) : SmartInitializingSingleton {

    override fun afterSingletonsInstantiated() {
        val botUser = bot.getMe()
            .getOrNull()
            ?: error("Failed to get bot info from Telegram API")

        val botUsername = (botUser.username ?: error("Bot has no username"))
            .removePrefix("@")

        botIdentity.username = botUsername
    }
}
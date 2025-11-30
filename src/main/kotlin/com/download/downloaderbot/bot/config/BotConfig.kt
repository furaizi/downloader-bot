package com.download.downloaderbot.bot.config

import com.download.downloaderbot.bot.commands.CommandContext
import com.download.downloaderbot.bot.config.properties.BotIdentity
import com.download.downloaderbot.bot.config.properties.BotProperties
import com.download.downloaderbot.bot.core.UpdateHandler
import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.text
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class BotConfig(
    val botProperties: BotProperties,
    val botScope: CoroutineScope,
    private val updateHandler: UpdateHandler,
) {
    @Bean
    fun telegramBot(): Bot =
        bot {
            token = botProperties.token

            dispatch {
                text {
                    update.consume()
                    val args = text.trim().split("\\s+".toRegex())
                    botScope.launch(ConcurrencyConfig.BotContext(CommandContext(update, args))) {
                        updateHandler.handle(update)
                    }
                }
            }
        }

    @Bean
    fun botIdentity() = BotIdentity("<uninitialized>")
}

package com.download.downloaderbot.bot.config

import com.download.downloaderbot.bot.commands.CommandContext
import com.download.downloaderbot.bot.config.properties.BotIdentity
import com.download.downloaderbot.bot.config.properties.BotProperties
import com.download.downloaderbot.bot.core.UpdateHandler
import com.download.downloaderbot.bot.exception.BotErrorGuard
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
    private val props: BotProperties,
    private val botScope: CoroutineScope,
    private val errorGuard: BotErrorGuard,
) {
    @Bean
    fun telegramBot(updateHandler: UpdateHandler): Bot =
        bot {
            token = props.token

            dispatch {
                text {
                    update.consume()
                    val args = text.trim().split("\\s+".toRegex())
                    val ctx = CommandContext(update, args)
                    botScope.launch {
                        errorGuard.runSafely(ctx) {
                            updateHandler.handle(update)
                        }
                    }
                }
            }
        }

    @Bean
    fun botIdentity() = BotIdentity("<uninitialized>")
}

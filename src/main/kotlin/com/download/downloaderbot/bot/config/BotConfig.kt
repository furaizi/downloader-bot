package com.download.downloaderbot.bot.config

import com.download.downloaderbot.bot.commands.BotCommand
import com.download.downloaderbot.bot.commands.CommandContext
import com.download.downloaderbot.bot.commands.CommandRegistry
import com.download.downloaderbot.bot.config.properties.BotIdentity
import com.download.downloaderbot.bot.config.properties.BotProperties
import com.download.downloaderbot.bot.core.UpdateHandler
import com.download.downloaderbot.bot.gateway.telegram.util.CommandAddressing
import com.download.downloaderbot.bot.gateway.telegram.util.addressing
import com.download.downloaderbot.infra.metrics.BotMetrics
import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.Dispatcher
import com.github.kotlintelegrambot.dispatcher.command
import com.github.kotlintelegrambot.dispatcher.handlers.CommandHandlerEnvironment
import com.github.kotlintelegrambot.dispatcher.handlers.TextHandlerEnvironment
import com.github.kotlintelegrambot.dispatcher.text
import com.github.kotlintelegrambot.entities.Update
import io.micrometer.core.instrument.Timer
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

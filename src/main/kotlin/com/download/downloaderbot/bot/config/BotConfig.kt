package com.download.downloaderbot.bot.config

import com.download.downloaderbot.bot.commands.BotCommand
import com.download.downloaderbot.bot.commands.CommandContext
import com.download.downloaderbot.bot.commands.CommandRegistry
import com.download.downloaderbot.bot.config.properties.BotProperties
import com.download.downloaderbot.infra.metrics.BotMetrics
import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.command
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
    val commands: CommandRegistry,
    private val botMetrics: BotMetrics,
) {
    @Bean
    fun telegramBot(): Bot =
        bot {
            token = botProperties.token

            dispatch {
                commands.byName.forEach { (name, handler) ->
                    command(name) {
                        botScope.launchHandler(update, args, handler)
                    }
                }

                text {
                    val args = text.trim().split("\\s+".toRegex())
                    botScope.launchHandler(update, args, commands.default)
                }
            }
        }

    private fun CoroutineScope.launchHandler(
        update: Update,
        args: List<String>,
        handler: BotCommand,
    ) {
        val ctx = CommandContext(update, args)
        update.consume()
        botMetrics.updates.increment()
        launch(ConcurrencyConfig.BotContext(ctx)) {
            val sample = Timer.start()
            try {
                botMetrics.commandCounter(handler.name).increment()
                handler.handle(ctx)
            } finally {
                sample.stop(botMetrics.commandTimer(handler.name))
            }
        }
    }
}

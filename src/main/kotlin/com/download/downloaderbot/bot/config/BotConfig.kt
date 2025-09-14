package com.download.downloaderbot.bot.config

import com.download.downloaderbot.bot.commands.CommandContext
import com.download.downloaderbot.bot.commands.BotCommand
import com.download.downloaderbot.bot.commands.CommandRegistry
import com.download.downloaderbot.bot.config.properties.BotProperties
import com.download.downloaderbot.bot.ratelimit.DefaultRateLimitGuard
import com.download.downloaderbot.bot.ratelimit.RateLimitGuard
import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.command
import com.github.kotlintelegrambot.dispatcher.text
import com.github.kotlintelegrambot.entities.Update
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mu.KotlinLogging
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

private val log = KotlinLogging.logger {}

@Configuration
class BotConfig(
    val botProperties: BotProperties,
    val scope: CoroutineScope,
    val commands: CommandRegistry,
    val rateLimitGuard: RateLimitGuard
) {

    @Bean
    fun telegramBot(): Bot = bot {
        token = botProperties.token

        dispatch {
            commands.byName.forEach { (name, handler) ->
                command(name) {
                    log.info { "Executing command /$name with args: $args" }
                    scope.launchHandler(update, args, handler)
                }
            }

            text {
                log.info { "Executing default command with text: '${text}'" }
                scope.launchHandler(update, listOf(text), commands.default)
            }
        }
    }

    private fun CoroutineScope.launchHandler(
        update: Update,
        args: List<String>,
        handler: BotCommand
    ) {
        val ctx = CommandContext(update, args)
        update.consume()
        launch(ConcurrencyConfig.BotContext(ctx)) {
            rateLimitGuard.runOrReject(ctx) {
                handler.handle(ctx)
            }
        }
    }

}
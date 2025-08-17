package com.download.downloaderbot.bot.config

import com.download.downloaderbot.bot.commands.CommandContext
import com.download.downloaderbot.bot.commands.CommandHandler
import com.download.downloaderbot.bot.commands.TelegramGateway
import com.download.downloaderbot.bot.config.properties.BotProperties
import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.command
import com.github.kotlintelegrambot.dispatcher.text
import mu.KotlinLogging
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

private val log = KotlinLogging.logger {}

@Configuration
class BotConfig(
    val botProperties: BotProperties,
    val commands: List<CommandHandler>,
    val gateway: TelegramGateway
) {

    private val defaultCommand = commands.first { it.name == "download" }

    @Bean
    fun telegramBot(): Bot = bot {
        token = botProperties.token
        dispatch {
            commands.forEach { handler ->
                command(handler.name) {
                    log.info { "Executing command /${handler.name}" }
                    handler.handle(CommandContext(update, args, gateway))
                }
            }

            text {
                defaultCommand.handle(CommandContext(update, listOf(text), gateway))
            }
        }
    }
}
package com.download.downloaderbot.bot.config

import com.download.downloaderbot.bot.commands.CommandContext
import com.download.downloaderbot.bot.commands.CommandHandler
import com.download.downloaderbot.bot.commands.TelegramGateway
import com.download.downloaderbot.bot.commands.chatId
import com.download.downloaderbot.bot.config.properties.BotProperties
import com.download.downloaderbot.bot.handler.GlobalTelegramExceptionHandler
import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.command
import com.github.kotlintelegrambot.dispatcher.text
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mu.KotlinLogging
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.UUID

private val log = KotlinLogging.logger {}

@Configuration
class BotConfig(
    val botProperties: BotProperties,
    val commands: List<CommandHandler>,
    val exceptionHandler: GlobalTelegramExceptionHandler
) {

    private val defaultCommand = commands.first { it.name == "download" }

    @Bean
    fun telegramBot(): Bot = bot {
        token = botProperties.token

        dispatch {
            commands.forEach { handler ->
                command(handler.name) {
                    log.info { "Executing command /${handler.name} with args: $args" }
                    handler.safeHandle(CommandContext(update, args))
                    update.consume()
                }
            }

            text {
                log.info { "Executing default command with text: '${text}'" }
                defaultCommand.safeHandle(CommandContext(update, listOf(text)))
            }
        }
    }

    private suspend fun CommandHandler.safeHandle(ctx: CommandContext) {
        try {
            handle(ctx)
        } catch (e: Exception) {
            exceptionHandler.handle(e, ctx)
        }
    }
}
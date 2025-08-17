package com.download.downloaderbot.bot.config

import com.download.downloaderbot.bot.commands.CommandContext
import com.download.downloaderbot.bot.commands.CommandHandler
import com.download.downloaderbot.bot.commands.TelegramGateway
import com.download.downloaderbot.bot.commands.chatId
import com.download.downloaderbot.bot.config.properties.BotProperties
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
    val gateway: TelegramGateway,
    private val botScope: CoroutineScope
) {

    private val defaultCommand = commands.first { it.name == "download" }

    @Bean
    fun telegramBot(): Bot = bot {
        token = botProperties.token
        dispatch {
            commands.forEach { handler ->
                command(handler.name) {
                    log.info { "Executing command /${handler.name} with args: $args" }
                    handler.safeHandle(CommandContext(update, args, gateway))
                }
            }

            text {
                defaultCommand.safeHandle(CommandContext(update, listOf(text), gateway))
            }
        }
    }

    private fun CommandHandler.safeHandle(ctx: CommandContext) {
        botScope.launch {
            try {
                handle(ctx)
            } catch (e: RuntimeException) {
                val errId = UUID.randomUUID().toString().take(8)
                log.error(e) { "Unhandled error id=$errId in /${name}" }
                runCatching {
                    gateway.replyText(
                        ctx.chatId,
                        "An error occurred while handling a command (id=$errId). Check log for a detailed message."
                    )
                }
            }
        }
    }
}
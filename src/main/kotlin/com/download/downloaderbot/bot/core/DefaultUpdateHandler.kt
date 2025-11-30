package com.download.downloaderbot.bot.core

import com.download.downloaderbot.bot.commands.BotCommand
import com.download.downloaderbot.bot.commands.CommandContext
import com.download.downloaderbot.bot.commands.CommandRegistry
import com.download.downloaderbot.bot.config.properties.BotIdentity
import com.download.downloaderbot.bot.gateway.telegram.util.CommandAddressing
import com.download.downloaderbot.bot.gateway.telegram.util.addressing
import com.download.downloaderbot.infra.metrics.BotMetrics
import com.github.kotlintelegrambot.entities.Update
import io.micrometer.core.instrument.Timer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Component

@Component
class DefaultUpdateHandler(
    private val commandRegistry: CommandRegistry,
    private val botIdentity: BotIdentity,
    private val botMetrics: BotMetrics,
) : UpdateHandler {

    override suspend fun handle(update: Update) {
        if (update.addressing(botIdentity.username) == CommandAddressing.OTHER) return

        val (handler, args) = update.resolveHandler() ?: return
        val context = CommandContext(update, args)

        botMetrics.updates.increment()
        val sample = Timer.start()
        try {
            botMetrics.commandCounter(handler.name).increment()

            withContext(Dispatchers.IO) {
                handler.handle(context)
            }
        } finally {
            sample.stop(botMetrics.commandTimer(handler.name))
        }
    }

    private fun Update.resolveHandler(): Pair<BotCommand, List<String>>? {
        val tokens = message
            ?.text
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?.split(WHITESPACE_REGEX)
            ?: return null

        val firstToken = tokens.first()

        return if (firstToken.startsWith(COMMAND_PREFIX)) {
            // /download@username_bot arg1 arg2
            val commandName = firstToken
                .removePrefix(COMMAND_PREFIX)
                .substringBefore(USERNAME_SEPARATOR)

            val handler = commandRegistry.byName[commandName] ?: commandRegistry.default
            handler to tokens.drop(1)
        } else {
            // default text
            val handler = commandRegistry.default
            handler to tokens
        }
    }

    private companion object {
        private val WHITESPACE_REGEX = Regex("\\s+")
        private const val COMMAND_PREFIX = "/"
        private const val USERNAME_SEPARATOR = '@'
    }
}

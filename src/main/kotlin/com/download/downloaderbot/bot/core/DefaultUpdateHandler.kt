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
    private val commands: CommandRegistry,
    private val botIdentity: BotIdentity,
    private val botMetrics: BotMetrics,
) : UpdateHandler {
    override suspend fun handle(update: Update) {
        if (update.addressing(botIdentity.username) == CommandAddressing.OTHER) {
            return
        }

        val (handler, args) = resolveHandler(update) ?: return

        val ctx = CommandContext(update, args)

        botMetrics.updates.increment()
        val sample = Timer.start()
        try {
            botMetrics.commandCounter(handler.name).increment()

            withContext(Dispatchers.IO) {
                handler.handle(ctx)
            }
        } finally {
            sample.stop(botMetrics.commandTimer(handler.name))
        }
    }

    private fun resolveHandler(update: Update): Pair<BotCommand, List<String>>? {
        val message = update.message ?: return null
        val rawText = message.text?.trim().orEmpty()
        if (rawText.isEmpty()) return null

        val tokens = rawText.split("\\s+".toRegex())
        if (tokens.isEmpty()) return null

        val first = tokens.first()

        return if (first.startsWith("/")) {
            // /download@username_bot arg1 arg2
            val cmdName =
                first
                    .removePrefix("/")
                    .substringBefore("@")

            val handler = commands.byName[cmdName] ?: commands.default
            val args = tokens.drop(1)
            handler to args
        } else {
            // default text
            val handler = commands.default
            handler to tokens
        }
    }
}

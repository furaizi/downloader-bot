package com.download.downloaderbot.bot.config

import com.download.downloaderbot.bot.commands.CommandHandler
import com.download.downloaderbot.bot.config.properties.BotProperties
import org.springframework.stereotype.Component

@Component
class CommandRegistry(
    handlers: List<CommandHandler>,
    props: BotProperties
) {
    val byName: Map<String, CommandHandler> = handlers.associateBy { it.name }
    val default: CommandHandler = byName[props.defaultCommand]
        ?: error("Default command handler not found: ${props.defaultCommand}")
}
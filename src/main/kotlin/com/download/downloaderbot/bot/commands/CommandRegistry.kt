package com.download.downloaderbot.bot.commands

import com.download.downloaderbot.bot.config.properties.BotProperties
import org.springframework.stereotype.Component

@Component
class CommandRegistry(
    handlers: List<BotCommand>,
    props: BotProperties
) {
    val byName: Map<String, BotCommand> = handlers.associateBy { it.name }
    val default: BotCommand = byName[props.defaultCommand]
        ?: error("Default command handler not found: ${props.defaultCommand}")
}
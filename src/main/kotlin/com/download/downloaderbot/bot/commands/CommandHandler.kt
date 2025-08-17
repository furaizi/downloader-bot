package com.download.downloaderbot.bot.commands

import com.github.kotlintelegrambot.entities.Update

abstract class CommandHandler(val gateway: TelegramGateway) {
    abstract val name: String
    abstract suspend fun handle(ctx: CommandContext)
}

data class CommandContext(
    val update: Update,
    val args: List<String>
)
package com.download.downloaderbot.bot.commands

import com.github.kotlintelegrambot.entities.Update

interface BotCommand {
    val name: String

    suspend fun handle(ctx: CommandContext)
}

data class CommandContext(
    val update: Update,
    val args: List<String>,
)

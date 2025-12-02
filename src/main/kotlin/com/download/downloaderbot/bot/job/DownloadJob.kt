package com.download.downloaderbot.bot.job

import com.download.downloaderbot.bot.commands.CommandContext
import java.util.UUID

data class DownloadJob(
    val id: UUID = UUID.randomUUID(),
    val sourceUrl: String,
    val chatId: Long,
    val replyToMessageId: Long?,
    val commandContext: CommandContext,
)

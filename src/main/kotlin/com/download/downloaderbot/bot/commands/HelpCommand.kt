package com.download.downloaderbot.bot.commands

import com.download.downloaderbot.bot.gateway.TelegramGateway
import com.download.downloaderbot.bot.gateway.chatId
import org.springframework.stereotype.Component

@Component
class HelpCommand(private val gateway: TelegramGateway) : BotCommand {
    override val name = "help"
    override suspend fun handle(ctx: CommandContext) {
        gateway.replyText(
            ctx.chatId,
            """
            /start – приветствие
            /help – помощь
            (просто пришли ссылку на видео в ЛС)
            """.trimIndent()
        )
    }
}
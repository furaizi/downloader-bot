package com.download.downloaderbot.bot.commands

import org.springframework.stereotype.Component

@Component
class HelpCommand(gateway: TelegramGateway) : CommandHandler(gateway) {
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
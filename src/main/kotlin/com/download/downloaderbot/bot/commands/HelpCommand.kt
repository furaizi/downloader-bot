package com.download.downloaderbot.bot.commands

class HelpCommand : CommandHandler {
    override val name = "help"
    override suspend fun handle(ctx: CommandContext) {
        ctx.gateway.replyText(
            ctx.chatId,
            """
            /start – приветствие
            /help – помощь
            (просто пришли ссылку на видео в ЛС)
            """.trimIndent()
        )
    }
}
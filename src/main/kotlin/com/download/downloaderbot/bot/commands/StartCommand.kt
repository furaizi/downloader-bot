package com.download.downloaderbot.bot.commands

import org.springframework.stereotype.Component

@Component
class StartCommand(gateway: TelegramGateway) : CommandHandler(gateway) {
    override val name = "start"
    override suspend fun handle(ctx: CommandContext) {
        gateway.replyText(ctx.chatId, "Привет! Отправь мне ссылку — я скачаю и пришлю видео.")
    }
}
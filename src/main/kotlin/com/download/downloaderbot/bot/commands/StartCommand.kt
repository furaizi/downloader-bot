package com.download.downloaderbot.bot.commands

import org.springframework.stereotype.Component

@Component
class StartCommand : CommandHandler {
    override val name = "start"
    override suspend fun handle(ctx: CommandContext) {
        ctx.gateway.replyText(ctx.chatId, "Привет! Отправь мне ссылку — я скачаю и пришлю видео.")
    }
}
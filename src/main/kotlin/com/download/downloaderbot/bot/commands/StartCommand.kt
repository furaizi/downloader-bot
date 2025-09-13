package com.download.downloaderbot.bot.commands

import com.download.downloaderbot.bot.gateway.TelegramGateway
import com.download.downloaderbot.bot.gateway.chatId
import org.springframework.stereotype.Component

@Component
class StartCommand(private val gateway: TelegramGateway) : BotCommand {
    override val name = "start"
    override suspend fun handle(ctx: CommandContext) {
        gateway.replyText(ctx.chatId, "Привет! Отправь мне ссылку — я скачаю и пришлю видео.")
    }
}
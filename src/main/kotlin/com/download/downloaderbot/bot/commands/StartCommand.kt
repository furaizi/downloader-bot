package com.download.downloaderbot.bot.commands

import com.download.downloaderbot.bot.gateway.TelegramGateway
import com.download.downloaderbot.bot.gateway.chatId
import com.download.downloaderbot.bot.ratelimit.guard.RateLimitGuard
import org.springframework.stereotype.Component

@Component
class StartCommand(
    private val gateway: TelegramGateway,
    private val rateLimitGuard: RateLimitGuard
) : BotCommand {
    override val name = "start"
    override suspend fun handle(ctx: CommandContext) {
        rateLimitGuard.runOrReject(ctx) {
            gateway.replyText(ctx.chatId, "Привіт! Надішли мені посилання - я завантажу і відправлю відео.")
        }
    }
}
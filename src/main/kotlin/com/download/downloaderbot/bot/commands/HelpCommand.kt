package com.download.downloaderbot.bot.commands

import com.download.downloaderbot.bot.gateway.BotPort
import com.download.downloaderbot.bot.gateway.chatId
import com.download.downloaderbot.bot.ratelimit.guard.RateLimitGuard
import org.springframework.stereotype.Component

@Component
class HelpCommand(
    private val botPort: BotPort,
    private val rateLimitGuard: RateLimitGuard
) : BotCommand {
    override val name = "help"
    override suspend fun handle(ctx: CommandContext) {
        rateLimitGuard.runOrReject(ctx) {
            botPort.sendText(
                ctx.chatId,
                """
            /start – привітання
            /help – допомога
            (просто надішли мені посилання на відео в особисті повідомлення)
            """.trimIndent()
            )
        }
    }
}
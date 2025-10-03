package com.download.downloaderbot.bot.commands

import com.download.downloaderbot.bot.gateway.BotPort
import com.download.downloaderbot.bot.gateway.telegram.chatId
import com.download.downloaderbot.bot.ratelimit.guard.RateLimitGuard
import mu.KotlinLogging
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

@Component
class HelpCommand(
    private val botPort: BotPort,
    private val rateLimitGuard: RateLimitGuard,
) : BotCommand {
    override val name = "help"

    override suspend fun handle(ctx: CommandContext) {
        log.info { "Executing command /$name" }
        rateLimitGuard.runOrReject(ctx) {
            botPort.sendText(
                ctx.chatId,
                """
                /start – привітання
                /help – допомога
                (просто надішли мені посилання на відео в особисті повідомлення)
                """.trimIndent(),
            )
        }
    }
}

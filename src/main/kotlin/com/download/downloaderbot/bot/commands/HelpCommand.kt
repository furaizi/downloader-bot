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
                /download <URL> – завантажити медіафайл за посиланням
                /stories <Instagram username> – завантажити актуальні історії Instagram користувача
                Або можеш просто надіслати посилання на медіафайл, і я спробую його завантажити!
                """.trimIndent(),
            )
        }
    }
}

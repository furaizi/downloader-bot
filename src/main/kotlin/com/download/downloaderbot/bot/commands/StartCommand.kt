package com.download.downloaderbot.bot.commands

import com.download.downloaderbot.bot.config.properties.BotIdentity
import com.download.downloaderbot.bot.config.properties.BotProperties
import com.download.downloaderbot.bot.gateway.telegram.chatId
import com.download.downloaderbot.bot.ratelimit.guard.RateLimitGuard
import com.download.downloaderbot.bot.ui.shareKeyboard
import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.ChatId
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

@Component
class StartCommand(
    private val bot: Bot,
    private val rateLimitGuard: RateLimitGuard,
    private val props: BotProperties,
    private val botIdentity: BotIdentity,
) : BotCommand {
    override val name = "start"

    private val share by lazy { shareKeyboard(botIdentity.username, props.shareText) }

    override suspend fun handle(ctx: CommandContext) {
        log.info { "Executing command /$name" }
        rateLimitGuard.runOrReject(ctx) {
            bot.sendMessage(
                chatId = ChatId.fromId(ctx.chatId),
                text = "Привіт! Надішли мені посилання - я завантажу і відправлю відео.",
                replyMarkup = share,
            )
        }
    }
}

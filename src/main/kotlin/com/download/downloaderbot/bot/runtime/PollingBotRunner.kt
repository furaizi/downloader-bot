package com.download.downloaderbot.bot.runtime

import com.github.kotlintelegrambot.Bot
import jakarta.annotation.PreDestroy
import mu.KotlinLogging
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component


private val log = KotlinLogging.logger {}

@Component
class PollingBotRunner(private val bot: Bot) : ApplicationRunner {

    override fun run(args: ApplicationArguments) {
        // Важно: при активном вебхуке polling не работает — снимаем вебхук.
        runCatching {
            bot.deleteWebhook(dropPendingUpdates = true)
            log.info { "Webhook deleted (dropPendingUpdates=true)" }
        }.onFailure {
            log.warn(it) { "deleteWebhook failed (continuing anyway)" }
        }

        bot.startPolling()
        log.info { "Telegram bot started polling" }
    }

    @PreDestroy
    fun onShutdown() {
        runCatching { bot.stopPolling() }
        log.info { "Telegram bot stopped polling" }
    }
}
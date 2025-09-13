package com.download.downloaderbot.bot.lifecycle

import com.github.kotlintelegrambot.Bot
import mu.KotlinLogging
import org.springframework.context.SmartLifecycle
import org.springframework.stereotype.Component
import java.util.concurrent.atomic.AtomicBoolean


private val log = KotlinLogging.logger {}

@Component
class PollingBotLifecycle(private val bot: Bot) : SmartLifecycle {

    private val running = AtomicBoolean(false)

    override fun isRunning(): Boolean = running.get()

    // Start late (after most beans), stop early on shutdown
    override fun getPhase(): Int = 0

    override fun start() {
        if (running.compareAndSet(false, true)) {
            runCatching {
                // Important: polling won't work if a webhook is active
                bot.deleteWebhook(dropPendingUpdates = true)
                log.info { "Webhook deleted (dropPendingUpdates=true)" }
            }.onFailure {
                log.warn(it) { "deleteWebhook failed (continuing anyway)" }
            }

            bot.startPolling()
            log.info { "Telegram bot started polling" }
        }
    }

    override fun stop() {
        if (running.compareAndSet(true, false)) {
            runCatching { bot.stopPolling() }
            .onFailure { log.warn(it) { "stopPolling failed" } }
            log.info { "Telegram bot stopped polling" }
        }
    }
}
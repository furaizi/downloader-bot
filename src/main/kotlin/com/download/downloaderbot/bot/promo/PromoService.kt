package com.download.downloaderbot.bot.promo

import com.download.downloaderbot.bot.config.properties.BotProperties
import org.springframework.stereotype.Component

@Component
class PromoService(
    private val props: BotProperties,
    private val counter: PromoCounter
) {
    fun shouldSend(chatId: Long): Boolean {
        if (props.promoText.isBlank())
            return false
        val n = props.promoEveryN.coerceAtLeast(1)
        val current = counter.incrementAndGet(chatId)
        return current % n == 0L
    }
}
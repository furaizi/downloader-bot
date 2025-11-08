package com.download.downloaderbot.bot.promo

import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

interface PromoCounter {
    fun incrementAndGet(chatId: Long): Long
}

@Component
class InMemoryPromoCounter : PromoCounter {
    private val counts = ConcurrentHashMap<Long, Long>()

    override fun incrementAndGet(chatId: Long): Long = counts.merge(chatId, 1L, java.lang.Long::sum)!!
}

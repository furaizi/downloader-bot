package com.download.downloaderbot.bot.promo

import com.download.downloaderbot.bot.config.properties.BotProperties
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PromoServiceTest {
    private val chatId = 12345L

    @Test
    fun `returns false when promo text is blank`() {
        val service = createService(promoText = "", promoEveryN = 1)

        assertFalse(service.shouldSend(chatId))
    }

    @Test
    fun `returns true on every call when promoEveryN is 1`() {
        val service = createService(promoText = "Check out our channel!", promoEveryN = 1)

        assertTrue(service.shouldSend(chatId))
        assertTrue(service.shouldSend(chatId))
        assertTrue(service.shouldSend(chatId))
    }

    @Test
    fun `returns true every Nth call`() {
        val service = createService(promoText = "Promo!", promoEveryN = 3)

        assertFalse(service.shouldSend(chatId)) // 1
        assertFalse(service.shouldSend(chatId)) // 2
        assertTrue(service.shouldSend(chatId)) // 3
        assertFalse(service.shouldSend(chatId)) // 4
        assertFalse(service.shouldSend(chatId)) // 5
        assertTrue(service.shouldSend(chatId)) // 6
    }

    @Test
    fun `tracks counts per chat independently`() {
        val service = createService(promoText = "Promo!", promoEveryN = 2)
        val chatId1 = 111L
        val chatId2 = 222L

        assertFalse(service.shouldSend(chatId1)) // chat1: count=1, 1%2!=0 -> false
        assertFalse(service.shouldSend(chatId2)) // chat2: count=1, 1%2!=0 -> false
        assertTrue(service.shouldSend(chatId1)) // chat1: count=2, 2%2==0 -> true
        assertTrue(service.shouldSend(chatId2)) // chat2: count=2, 2%2==0 -> true (independent counter)
    }

    @Test
    fun `treats zero or negative promoEveryN as 1`() {
        val serviceZero = createService(promoText = "Promo!", promoEveryN = 0)
        val serviceNegative = createService(promoText = "Promo!", promoEveryN = -5)

        assertTrue(serviceZero.shouldSend(chatId))
        assertTrue(serviceNegative.shouldSend(100L))
    }

    private fun createService(
        promoText: String,
        promoEveryN: Int,
    ): PromoService {
        val props =
            mockk<BotProperties> {
                every { this@mockk.promoText } returns promoText
                every { this@mockk.promoEveryN } returns promoEveryN
            }
        val counter = InMemoryPromoCounter()
        return PromoService(props, counter)
    }
}

package com.download.downloaderbot.bot.promo

import com.download.downloaderbot.bot.config.properties.BotProperties
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class PromoServiceTest : FunSpec({

    lateinit var counter: PromoCounter

    fun promoService(
        promoText: String = "Some promo",
        promoEveryN: Int = 1,
    ): PromoService =
        PromoService(
            props = BotProperties(
                token = "dummy-token",
                defaultCommand = "download",
                shareText = "share",
                promoText = promoText,
                promoEveryN = promoEveryN,
            ),
            counter = counter,
        )

    beforeTest {
        counter = mockk()
    }

    context("when promo text is blank") {

        test("never sends promo and does not touch counter") {
            val service = promoService(promoText = "   ")

            val result = service.shouldSend(chatId = 123L)

            result shouldBe false
            verify { counter wasNot called }
        }
    }

    context("when promo text is present") {

        test("sends each nth message for a given chat when promoEveryN > 1") {
            val service = promoService(
                promoText = "Buy our premium!",
                promoEveryN = 3,
            )

            every { counter.incrementAndGet(1L) } returnsMany listOf(1L, 2L, 3L, 4L, 5L, 6L)

            val results = (1..6).map { service.shouldSend(chatId = 1L) }

            results shouldBe listOf(
                false, // 1 % 3 != 0
                false, // 2 % 3 != 0
                true,  // 3 % 3 == 0
                false,
                false,
                true,  // 6 % 3 == 0
            )

            verify(exactly = 6) { counter.incrementAndGet(1L) }
        }

        test("sends promo on every message when promoEveryN is 1") {
            val service = promoService(
                promoText = "Premium every time",
                promoEveryN = 1,
            )

            every { counter.incrementAndGet(42L) } returnsMany listOf(1L, 2L, 3L)

            val results = (1..3).map { service.shouldSend(chatId = 42L) }

            results shouldBe listOf(true, true, true)
            verify(exactly = 3) { counter.incrementAndGet(42L) }
        }

        test("treats non-positive promoEveryN as 1 (coerceAtLeast(1))") {
            val service = promoService(
                promoText = "Non-positive N",
                promoEveryN = 0,
            )

            every { counter.incrementAndGet(777L) } returnsMany listOf(1L, 2L, 3L)

            val results = (1..3).map { service.shouldSend(chatId = 777L) }

            results shouldBe listOf(true, true, true)
            verify(exactly = 3) { counter.incrementAndGet(777L) }
        }

        test("uses independent counters per chat id") {
            val service = promoService(
                promoText = "Every 2nd message",
                promoEveryN = 2,
            )

            every { counter.incrementAndGet(10L) } returnsMany listOf(1L, 2L)
            every { counter.incrementAndGet(20L) } returnsMany listOf(1L, 2L)

            // first pass through two chats
            service.shouldSend(10L) shouldBe false // 1 % 2 != 0
            service.shouldSend(20L) shouldBe false // 1 % 2 != 0

            // second pass through the same chats
            service.shouldSend(10L) shouldBe true  // 2 % 2 == 0
            service.shouldSend(20L) shouldBe true  // 2 % 2 == 0

            verify(exactly = 2) { counter.incrementAndGet(10L) }
            verify(exactly = 2) { counter.incrementAndGet(20L) }
        }
    }
})

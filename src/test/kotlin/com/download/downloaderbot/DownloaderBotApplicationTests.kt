package com.download.downloaderbot

import com.download.downloaderbot.bot.config.TestBotConfig
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles

@Import(TestBotConfig::class)
@SpringBootTest
@ActiveProfiles("test")
class DownloaderBotApplicationTests {
    @Test
    fun contextLoads() {
        assertTrue(true)
    }
}

package com.download.downloaderbot.app.download

import com.download.downloaderbot.bot.config.TestBotConfig
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.testcontainers.junit.jupiter.Testcontainers

@Import(TestBotConfig::class)
@Testcontainers
@ActiveProfiles("test")
@SpringBootTest(properties = ["spring.config.location=classpath:/"])
class MediaServiceImplIT {
}
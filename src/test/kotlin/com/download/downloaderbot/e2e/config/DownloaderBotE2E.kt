package com.download.downloaderbot.e2e.config

import com.download.downloaderbot.bot.config.BotTestConfig
import com.download.downloaderbot.infra.config.RedisTestConfig
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.testcontainers.junit.jupiter.Testcontainers

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@SpringBootTest(
    classes = [
        BotTestConfig::class,
        RedisTestConfig::class,
        E2ETestConfig::class,
    ],
    properties = ["spring.config.location=classpath:/"],
)
@ActiveProfiles("test")
@Testcontainers
annotation class DownloaderBotE2E
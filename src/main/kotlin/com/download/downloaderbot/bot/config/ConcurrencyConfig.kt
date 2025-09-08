package com.download.downloaderbot.bot.config

import jakarta.annotation.PreDestroy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component

@Configuration
class ConcurrencyConfig {
    @Bean
    fun botScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
}

@Component
class BotScopeShutdown(private val scope: CoroutineScope) {
    @PreDestroy
    fun shutdown() = scope.cancel()
}
package com.download.downloaderbot.bot.config

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import mu.KotlinLogging
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

private val log = KotlinLogging.logger {}

@Configuration
class ConcurrencyConfig {

    @Bean
    fun botScope() = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Bean
    fun maintenanceScope() = CoroutineScope(SupervisorJob() + Dispatchers.IO)
}

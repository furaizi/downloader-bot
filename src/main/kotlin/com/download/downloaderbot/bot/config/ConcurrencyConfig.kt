package com.download.downloaderbot.bot.config

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ConcurrencyConfig {

    @Bean
    fun botScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

}
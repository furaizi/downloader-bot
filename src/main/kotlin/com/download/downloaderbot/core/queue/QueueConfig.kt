package com.download.downloaderbot.core.queue

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class QueueConfig {
    @Bean
    fun jobStore(): JobStore = InMemoryJobStore()
}
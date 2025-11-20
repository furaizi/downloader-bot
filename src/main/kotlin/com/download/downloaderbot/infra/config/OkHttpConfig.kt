package com.download.downloaderbot.infra.config

import okhttp3.OkHttpClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Duration

private const val OKHTTP_CALL_TIMEOUT_SECONDS = 20L

@Configuration
class OkHttpConfig {
    @Bean
    fun okHttpClient(): OkHttpClient =
        OkHttpClient.Builder()
            .followRedirects(true)
            .followSslRedirects(true)
            .callTimeout(Duration.ofSeconds(OKHTTP_CALL_TIMEOUT_SECONDS))
            .build()
}

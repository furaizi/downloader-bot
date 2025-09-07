package com.download.downloaderbot.core.config

import okhttp3.OkHttpClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Duration

@Configuration
class OkHttpConfig {

    @Bean
    fun okHttpClient(): OkHttpClient =
        OkHttpClient.Builder()
            .followRedirects(true)
            .followSslRedirects(true)
            .callTimeout(Duration.ofSeconds(20))
            .build()
}
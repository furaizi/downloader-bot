package com.download.downloaderbot.bot.config

import com.download.downloaderbot.bot.ratelimit.guard.NoopRateLimitGuard
import com.download.downloaderbot.bot.ratelimit.guard.RateLimitGuard
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@ConditionalOnProperty(
    prefix = "downloader.ratelimit",
    name = ["enabled"],
    havingValue = "false",
    matchIfMissing = true,
)
class NoopRateLimitConfiguration {
    @Bean
    @ConditionalOnMissingBean(RateLimitGuard::class)
    fun noopRateLimitGuard(): RateLimitGuard = NoopRateLimitGuard()
}

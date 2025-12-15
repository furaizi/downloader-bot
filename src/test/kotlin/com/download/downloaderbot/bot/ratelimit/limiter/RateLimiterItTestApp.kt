package com.download.downloaderbot.bot.ratelimit.limiter

import com.download.downloaderbot.bot.config.RedisRateLimitConfiguration
import com.download.downloaderbot.bot.config.RedisTestConfig
import com.download.downloaderbot.bot.config.properties.RateLimitProperties
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Import

@SpringBootConfiguration
@EnableAutoConfiguration
@EnableConfigurationProperties(RateLimitProperties::class)
@Import(RedisTestConfig::class, RedisRateLimitConfiguration::class)
class RateLimiterItTestApp
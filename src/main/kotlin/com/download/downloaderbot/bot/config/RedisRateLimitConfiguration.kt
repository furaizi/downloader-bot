package com.download.downloaderbot.bot.config

import com.download.downloaderbot.bot.config.properties.RateLimitProperties
import com.download.downloaderbot.bot.ratelimit.guard.RateLimitGuard
import com.download.downloaderbot.bot.ratelimit.limiter.Bucket4jRateLimiter
import com.download.downloaderbot.bot.ratelimit.limiter.RateLimiter
import com.fasterxml.jackson.databind.ObjectMapper
import io.github.bucket4j.distributed.proxy.ProxyManager
import io.github.bucket4j.redis.lettuce.Bucket4jLettuce
import io.lettuce.core.RedisClient
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.codec.ByteArrayCodec
import io.lettuce.core.codec.RedisCodec
import io.lettuce.core.codec.StringCodec
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory

@Configuration
class RedisRateLimitConfiguration {
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBooleanProperty(
        prefix = "downloader.ratelimit",
        name = ["enabled"],
    )
    fun redisConnection(connectionFactory: LettuceConnectionFactory): StatefulRedisConnection<String, ByteArray> {
        val client = connectionFactory.requiredNativeClient as RedisClient
        val codec: RedisCodec<String, ByteArray> = RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE)
        return client.connect(codec)
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBooleanProperty(
        prefix = "downloader.ratelimit",
        name = ["enabled"],
    )
    fun bucket4jProxyManager(connection: StatefulRedisConnection<String, ByteArray>): ProxyManager<String> =
        Bucket4jLettuce
            .casBasedBuilder(connection)
            .build()

    @Bean
    @ConditionalOnMissingBean(RateLimiter::class)
    @ConditionalOnBooleanProperty(
        prefix = "downloader.ratelimit",
        name = ["enabled"],
    )
    fun rateLimiter(
        proxyManager: ProxyManager<String>,
        props: RateLimitProperties,
        mapper: ObjectMapper,
    ): RateLimiter = Bucket4jRateLimiter(proxyManager, props, mapper)

    @Bean
    @ConditionalOnMissingBean
    fun rateLimitGuard(rateLimiterProvider: ObjectProvider<RateLimiter>): RateLimitGuard = RateLimitGuard(rateLimiterProvider.ifAvailable)
}

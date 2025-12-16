package com.download.downloaderbot.bot.config

import com.download.downloaderbot.bot.config.properties.RateLimitProperties
import com.download.downloaderbot.bot.ratelimit.guard.DefaultRateLimitGuard
import com.download.downloaderbot.bot.ratelimit.guard.RateLimitGuard
import com.download.downloaderbot.bot.ratelimit.limiter.Bucket4jRateLimiter
import com.download.downloaderbot.bot.ratelimit.limiter.RateLimiter
import com.fasterxml.jackson.databind.ObjectMapper
import io.github.bucket4j.distributed.proxy.ProxyManager
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager
import io.lettuce.core.RedisClient
import io.lettuce.core.RedisURI
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.codec.ByteArrayCodec
import io.lettuce.core.codec.RedisCodec
import io.lettuce.core.codec.StringCodec
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.data.redis.RedisConnectionDetails
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@ConditionalOnProperty(
    prefix = "downloader.ratelimit",
    name = ["enabled"],
    havingValue = "true",
)
class RedisRateLimitConfiguration {
    @Bean(destroyMethod = "shutdown")
    @ConditionalOnMissingBean
    fun redisClient(details: RedisConnectionDetails): RedisClient {
        val standalone = details.standalone
        val uri =
            RedisURI.Builder.redis(standalone.host, standalone.port)
                .apply {
                    val password = details.password
                    if (!password.isNullOrBlank()) withPassword(password.toCharArray())
                    if (standalone.sslBundle != null) withSsl(true)
                    withDatabase(standalone.database)
                }
                .build()

        return RedisClient.create(uri)
    }

    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean
    fun redisConnection(client: RedisClient): StatefulRedisConnection<String, ByteArray> {
        val codec: RedisCodec<String, ByteArray> = RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE)
        return client.connect(codec)
    }

    @Bean
    @ConditionalOnMissingBean
    fun bucket4jProxyManager(connection: StatefulRedisConnection<String, ByteArray>): ProxyManager<String> {
        return LettuceBasedProxyManager
            .builderFor(connection)
            .build()
    }

    @Bean
    @ConditionalOnMissingBean(RateLimiter::class)
    fun rateLimiter(
        proxyManager: ProxyManager<String>,
        props: RateLimitProperties,
        mapper: ObjectMapper,
    ): RateLimiter = Bucket4jRateLimiter(proxyManager, props, mapper)

    @Bean
    @ConditionalOnMissingBean(RateLimitGuard::class)
    fun rateLimitGuard(limiter: RateLimiter): RateLimitGuard = DefaultRateLimitGuard(limiter)
}

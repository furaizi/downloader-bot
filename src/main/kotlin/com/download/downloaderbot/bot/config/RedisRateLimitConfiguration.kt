package com.download.downloaderbot.bot.config

import com.download.downloaderbot.bot.config.properties.RateLimitProperties
import com.download.downloaderbot.bot.gateway.TelegramGateway
import com.download.downloaderbot.bot.ratelimit.guard.Bucket4jRateLimiter
import com.download.downloaderbot.bot.ratelimit.limiter.DefaultRateLimitGuard
import com.download.downloaderbot.bot.ratelimit.limiter.RateLimitGuard
import com.download.downloaderbot.bot.ratelimit.guard.RateLimiter
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
import org.springframework.boot.autoconfigure.data.redis.RedisProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@ConditionalOnProperty(
    prefix = "downloader.ratelimit",
    name = ["enabled"],
    havingValue = "true"
)
class RedisRateLimitConfiguration {


    @Bean(destroyMethod = "shutdown")
    @ConditionalOnMissingBean
    fun redisClient(redisProps: RedisProperties): RedisClient {
        val uri = RedisURI.Builder
            .redis(redisProps.host, redisProps.port)
            .apply {
                if (!redisProps.password.isNullOrBlank())
                    withPassword(redisProps.password.toCharArray())
                if (redisProps.ssl.isEnabled) withSsl(true)
                withDatabase(redisProps.database)
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
    fun bucket4jProxyManager(
        connection: StatefulRedisConnection<String, ByteArray>
    ): ProxyManager<String> {
        return LettuceBasedProxyManager
            .builderFor(connection)
            .build()
    }

    @Bean
    @ConditionalOnMissingBean(RateLimiter::class)
    fun rateLimiter(
        proxyManager: ProxyManager<String>,
        props: RateLimitProperties
    ): RateLimiter = Bucket4jRateLimiter(proxyManager, props)

    @Bean
    @ConditionalOnMissingBean(RateLimitGuard::class)
    fun rateLimitGuard(
        limiter: RateLimiter,
        gateway: TelegramGateway
    ): RateLimitGuard = DefaultRateLimitGuard(limiter, gateway)
}
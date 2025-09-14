package com.download.downloaderbot.bot.config

import io.github.bucket4j.distributed.proxy.ProxyManager
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager
import io.lettuce.core.RedisClient
import io.lettuce.core.RedisURI
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.codec.ByteArrayCodec
import io.lettuce.core.codec.RedisCodec
import io.lettuce.core.codec.StringCodec
import jakarta.annotation.PreDestroy
import org.springframework.boot.autoconfigure.data.redis.RedisProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class Bucket4jConfig(
    private val redisProps: RedisProperties
) {

    private lateinit var client: RedisClient
    private lateinit var connection: StatefulRedisConnection<String, ByteArray>

    @Bean
    fun bucket4jProxyManager(): ProxyManager<String> {
        val uri = RedisURI.Builder
            .redis(redisProps.host, redisProps.port)
            .apply {
                if (!redisProps.password.isNullOrBlank())
                    withPassword(redisProps.password.toCharArray())
                if (redisProps.ssl.isEnabled) withSsl(true)
                withDatabase(redisProps.database)
            }
            .build()

        val codec = RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE)

        client = RedisClient.create(uri)
        connection = client.connect(codec)

        return LettuceBasedProxyManager
            .builderFor(connection)
            .build()
    }

    @PreDestroy
    fun destroy() {
        runCatching { connection.close() }
        runCatching { client.shutdown() }
    }

}
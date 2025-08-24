package com.download.downloaderbot.core.config

import com.download.downloaderbot.core.domain.Media
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.core.ReactiveStringRedisTemplate
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializationContext
import org.springframework.data.redis.serializer.StringRedisSerializer

@Configuration
class RedisConfig {

    @Bean
    fun mediaRedisTemplate(
        factory: ReactiveRedisConnectionFactory,
        objectMapper: ObjectMapper
    ): ReactiveRedisTemplate<String, Media> {
        val key = StringRedisSerializer()
        val value = Jackson2JsonRedisSerializer(objectMapper, Media::class.java)
        val ctx = RedisSerializationContext
            .newSerializationContext<String, Media>(key)
            .value(value)
            .build()
        return ReactiveRedisTemplate(factory, ctx)
    }

    @Bean
    fun stringRedisTemplate(
        factory: ReactiveRedisConnectionFactory
    ) = ReactiveStringRedisTemplate(factory)
}
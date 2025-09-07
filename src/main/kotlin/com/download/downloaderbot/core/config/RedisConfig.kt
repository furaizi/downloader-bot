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
        mapper: ObjectMapper
    ): ReactiveRedisTemplate<String, List<Media>> {
        val key = StringRedisSerializer()
        val javaType = mapper.typeFactory
            .constructCollectionType(List::class.java, Media::class.java)
        val value = Jackson2JsonRedisSerializer<List<Media>>(mapper, javaType)

        val ctx = RedisSerializationContext
            .newSerializationContext<String, List<Media>>(key)
            .value(value)
            .build()
        return ReactiveRedisTemplate(factory, ctx)
    }

    @Bean
    fun reactiveStringRedisTemplate(
        factory: ReactiveRedisConnectionFactory
    ): ReactiveStringRedisTemplate = ReactiveStringRedisTemplate(factory)
}

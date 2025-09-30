package com.download.downloaderbot.infra.config

import com.download.downloaderbot.core.cache.CachedMedia
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
    fun cachedMediaRedisTemplate(
        factory: ReactiveRedisConnectionFactory,
        mapper: ObjectMapper
    ): ReactiveRedisTemplate<String, List<CachedMedia>> {
        val key = StringRedisSerializer()
        val javaType = mapper.typeFactory
            .constructCollectionType(List::class.java, CachedMedia::class.java)
        val value = Jackson2JsonRedisSerializer<List<CachedMedia>>(mapper, javaType)

        val ctx = RedisSerializationContext
            .newSerializationContext<String, List<CachedMedia>>(key)
            .value(value)
            .build()
        return ReactiveRedisTemplate(factory, ctx)
    }

    @Bean
    fun reactiveStringRedisTemplate(
        factory: ReactiveRedisConnectionFactory
    ): ReactiveStringRedisTemplate = ReactiveStringRedisTemplate(factory)
}

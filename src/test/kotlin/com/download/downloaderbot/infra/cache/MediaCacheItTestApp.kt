package com.download.downloaderbot.infra.cache

import com.download.downloaderbot.app.config.properties.CacheProperties
import com.download.downloaderbot.infra.config.MappingConfig
import com.download.downloaderbot.infra.config.RedisConfig
import com.download.downloaderbot.infra.config.RedisTestConfig
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Import

@SpringBootConfiguration
@EnableAutoConfiguration
@EnableConfigurationProperties(CacheProperties::class)
@Import(
    RedisTestConfig::class,
    MappingConfig::class,
    RedisConfig::class,
)
class MediaCacheItTestApp

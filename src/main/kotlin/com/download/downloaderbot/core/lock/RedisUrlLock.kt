package com.download.downloaderbot.core.lock

import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.core.io.ByteArrayResource
import org.springframework.data.redis.core.ReactiveStringRedisTemplate
import org.springframework.data.redis.core.script.RedisScript
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.util.UUID

@Component
class RedisUrlLock(
    private val redis: ReactiveStringRedisTemplate
) : UrlLockManager {

    private fun key(url: String) = "lock:url:$url"

    override suspend fun tryAcquire(url: String, ttl: Duration): String? {
        val token = UUID.randomUUID().toString()
        val acquired = redis.opsForValue()
            .setIfAbsent(key(url), token, ttl)
            .awaitFirstOrNull() ?: false
        return if (acquired) token else null
    }

    override suspend fun release(url: String, token: String) {
        val lua = """
            if redis.call('get', KEYS[1]) == ARGV[1] then
              return redis.call('del', KEYS[1])
            else
              return 0
            end
        """.trimIndent()
        val script: RedisScript<Long> = RedisScript.of(
            ByteArrayResource(lua.toByteArray(StandardCharsets.UTF_8)),
            Long::class.java
        )

        redis.execute(script, listOf(key(url)), token)
            .collectList()
            .awaitFirstOrNull()
    }
}


package com.download.downloaderbot.bot.ratelimit.limiter

import com.download.downloaderbot.bot.config.RedisRateLimitConfiguration
import com.download.downloaderbot.bot.config.RedisTestConfig
import com.download.downloaderbot.bot.config.properties.RateLimitProperties
import com.fasterxml.jackson.databind.ObjectMapper
import io.github.bucket4j.distributed.proxy.ProxyManager
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.longs.shouldBeGreaterThanOrEqual
import io.lettuce.core.RedisClient
import io.lettuce.core.RedisURI
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.codec.ByteArrayCodec
import io.lettuce.core.codec.RedisCodec
import io.lettuce.core.codec.StringCodec
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.data.redis.RedisConnectionDetails
import org.springframework.boot.autoconfigure.data.redis.RedisProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.time.Duration

@SpringBootTest(
    classes = [ RateLimiterItTestApp::class ],
    properties = [
        "downloader.ratelimit.enabled=true",
        "downloader.ratelimit.namespace=rl-it",
        "downloader.ratelimit.global.capacity=1",
        "downloader.ratelimit.global.refill.tokens=1",
        "downloader.ratelimit.global.refill.period=250ms",
        "downloader.ratelimit.global.refill.greedy=true",
        "downloader.ratelimit.chat.capacity=1",
        "downloader.ratelimit.chat.refill.tokens=1",
        "downloader.ratelimit.chat.refill.period=10s",
        "downloader.ratelimit.chat.refill.greedy=true",
        "downloader.ratelimit.group.capacity=2",
        "downloader.ratelimit.group.refill.tokens=2",
        "downloader.ratelimit.group.refill.period=10s",
        "downloader.ratelimit.group.refill.greedy=true",
    ],
)
@ActiveProfiles("test")
class Bucket4jRateLimiterIT : FunSpec() {

    override fun extensions() = listOf(SpringExtension)

    @Autowired lateinit var limiter: RateLimiter
    @Autowired lateinit var redisConnection: StatefulRedisConnection<String, ByteArray>
    @Autowired lateinit var proxyManager: ProxyManager<String>
    @Autowired lateinit var mapper: ObjectMapper
    @Autowired lateinit var redisProps: RedisProperties
    @Autowired lateinit var redisDetails: RedisConnectionDetails

    init {
        beforeTest {
            redisConnection.sync().flushall()
        }

        test("print redis props") {
            println("redisProps.host=${redisProps.host}, port=${redisProps.port}")
        }


        test("global: second awaitGlobal throttles until refill") {
            limiter.awaitGlobal()

            val startNs = System.nanoTime()
            limiter.awaitGlobal()
            val elapsedMs = (System.nanoTime() - startNs) / 1_000_000

            elapsedMs.shouldBeGreaterThanOrEqual(80)
        }

        test("per-chat: same chatId is limited, different chatId is independent") {
            limiter.tryConsumePerChatOrGroup(42).shouldBeTrue()
            limiter.tryConsumePerChatOrGroup(42).shouldBeFalse()

            limiter.tryConsumePerChatOrGroup(43).shouldBeTrue()
            limiter.tryConsumePerChatOrGroup(43).shouldBeFalse()
        }

        test("group vs chat: group has its own limit and separate keyspace") {
            limiter.tryConsumePerChatOrGroup(100).shouldBeTrue()
            limiter.tryConsumePerChatOrGroup(100).shouldBeFalse()

            limiter.tryConsumePerChatOrGroup(-100).shouldBeTrue()
            limiter.tryConsumePerChatOrGroup(-100).shouldBeTrue()
            limiter.tryConsumePerChatOrGroup(-100).shouldBeFalse()
        }

        test("versioning: changing RateLimitProperties changes fingerprint => separate buckets") {
            val base =
                RateLimitProperties(
                    true,
                    "rl-it-ver",
                    chat = RateLimitProperties.Bucket(
                        1,
                        RateLimitProperties.Refill(1, Duration.ofSeconds(10), true),
                    ),
                )

            val rl1 = Bucket4jRateLimiter(proxyManager, base, mapper)

            rl1.tryConsumePerChatOrGroup(777).shouldBeTrue()
            rl1.tryConsumePerChatOrGroup(777).shouldBeFalse()

            val changed =
                base.copy(
                    chat = base.chat.copy(
                        capacity = 2,
                        refill = base.chat.refill.copy(tokens = 2),
                    )
                )
            val rl2 = Bucket4jRateLimiter(proxyManager, changed, mapper)

            rl2.tryConsumePerChatOrGroup(777).shouldBeTrue()
            rl2.tryConsumePerChatOrGroup(777).shouldBeTrue()
            rl2.tryConsumePerChatOrGroup(777).shouldBeFalse()
        }

        test("fail-open: if Redis connection is closed, tryConsumePerChatOrGroup returns true") {
            val s = redisDetails.standalone
            val uri = RedisURI.Builder.redis(s.host, s.port)
                .apply {
                    val pwd = redisDetails.password
                    if (!pwd.isNullOrBlank()) withPassword(pwd.toCharArray())
                    if (s.sslBundle != null) withSsl(true)
                    withDatabase(s.database)
                }
                .build()

            val client = RedisClient.create(uri)
            val codec: RedisCodec<String, ByteArray> = RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE)
            val conn = client.connect(codec)

            val brokenProxyManager =
                LettuceBasedProxyManager
                    .builderFor(conn)
                    .build()

            val p =
                RateLimitProperties(
                    true,
                    "rl-it-failopen",
                    chat = RateLimitProperties.Bucket(
                        1,
                        RateLimitProperties.Refill(1, Duration.ofSeconds(10), true),
                    ),
                )
            val rl = Bucket4jRateLimiter(brokenProxyManager, p, mapper)

            conn.close()
            client.shutdown()

            rl.tryConsumePerChatOrGroup(1).shouldBeTrue()
        }
    }
}

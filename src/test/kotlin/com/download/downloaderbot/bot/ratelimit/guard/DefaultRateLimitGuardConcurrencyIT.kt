package com.download.downloaderbot.bot.ratelimit.guard

import com.download.downloaderbot.bot.commands.util.ctx
import com.download.downloaderbot.bot.ratelimit.limiter.RateLimiterItTestApp
import com.download.downloaderbot.core.downloader.TooManyRequestsException
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.ints.shouldBeLessThanOrEqual
import io.lettuce.core.api.StatefulRedisConnection
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

private const val NAMESPACE = "rl-concurrency-it"

@SpringBootTest(
    classes = [RateLimiterItTestApp::class],
    properties = [
        "downloader.ratelimit.enabled=true",
        "downloader.ratelimit.namespace=$NAMESPACE",
        "downloader.ratelimit.global.capacity=10000",
        "downloader.ratelimit.global.refill.tokens=10000",
        "downloader.ratelimit.global.refill.period=PT1S",
        "downloader.ratelimit.global.refill.greedy=true",
        "downloader.ratelimit.chat.capacity=3",
        "downloader.ratelimit.chat.refill.tokens=3",
        "downloader.ratelimit.chat.refill.period=PT10S",
        "downloader.ratelimit.chat.refill.greedy=true",
    ],
)
@ActiveProfiles("test")
class DefaultRateLimitGuardConcurrencyIT
    @Autowired
    constructor(
        private val guard: DefaultRateLimitGuard,
        private val redisConnection: StatefulRedisConnection<String, ByteArray>,
    ) : FunSpec({

            extension(SpringExtension)
            beforeTest {
                val sync = redisConnection.sync()
                val keys = sync.keys("$NAMESPACE*")
                if (keys.isNotEmpty()) {
                    sync.del(*keys.toTypedArray())
                }
            }

            test("caps per-chat burst under concurrent start") {
                val totalRequests = 1_000
                val ctx = ctx(chatId = 424242L)

                val results =
                    coroutineScope {
                        val start = CompletableDeferred<Unit>()

                        val tasks =
                            (1..totalRequests).map {
                                async(Dispatchers.IO) {
                                    start.await()
                                    runCatching {
                                        guard.runOrReject(ctx) { "ok" }
                                        true
                                    }.getOrElse { t ->
                                        if (t is TooManyRequestsException) false else throw t
                                    }
                                }
                            }

                        start.complete(Unit)
                        tasks.awaitAll()
                    }

                val accepted = results.count { it }
                accepted.shouldBeLessThanOrEqual(3)
            }
        })

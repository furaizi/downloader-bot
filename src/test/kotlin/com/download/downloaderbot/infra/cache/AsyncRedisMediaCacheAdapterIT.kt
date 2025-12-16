package com.download.downloaderbot.infra.cache

import com.download.downloaderbot.core.cache.CachePort
import com.download.downloaderbot.core.domain.Media
import com.download.downloaderbot.core.domain.MediaType
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.test.context.ActiveProfiles
import java.time.Duration
import java.time.OffsetDateTime
import kotlin.time.Duration.Companion.seconds

@SpringBootTest(
    classes = [MediaCacheItTestApp::class],
    properties = ["spring.config.location=classpath:/"],
)
@ActiveProfiles("test")
class AsyncRedisMediaCacheAdapterIT
    @Autowired
    constructor(
        private val cache: CachePort<String, List<Media>>,
        private val redisTemplate: ReactiveRedisTemplate<String, List<Media>>,
        private val connectionFactory: ReactiveRedisConnectionFactory,
    ) : FunSpec({

            extension(SpringExtension)

            fun url(path: String) = "https://example.com/$path"

            fun mediaFor(sourceUrl: String): List<Media> =
                listOf(
                    Media(
                        type = MediaType.VIDEO,
                        fileUrl = "file:///tmp/video.mp4",
                        sourceUrl = sourceUrl,
                        title = "Video",
                        fileUniqueId = "uniq-1",
                        lastFileId = "file-1",
                        downloadedAt = FIXED_DOWNLOADED_AT,
                    ),
                    Media(
                        type = MediaType.IMAGE,
                        fileUrl = "file:///tmp/1.jpg",
                        sourceUrl = sourceUrl,
                        title = "Img",
                        fileUniqueId = "uniq-2",
                        lastFileId = "file-2",
                        downloadedAt = FIXED_DOWNLOADED_AT,
                    ),
                )

            beforeEach {
                connectionFactory.reactiveConnection
                    .serverCommands()
                    .flushDb()
                    .awaitFirstOrNull()
            }

            context("get") {
                test("returns null when key is absent") {
                    cache.get(url("miss")).shouldBeNull()
                }
            }

            context("put/get") {
                test("stores and reads list back") {
                    val key = url("hit")
                    val value = mediaFor(key)

                    cache.put(key, value, DEFAULT_TTL)

                    cache.get(key) shouldBe value
                    cache.get(key)!!.shouldHaveSize(2)
                }

                test("does nothing when values are empty") {
                    val key = url("empty")

                    cache.put(key, emptyList(), DEFAULT_TTL)

                    cache.get(key).shouldBeNull()
                }
            }

            context("evict") {
                test("removes key") {
                    val key = url("evict")
                    cache.put(key, mediaFor(key), DEFAULT_TTL)

                    cache.get(key)!!.shouldHaveSize(2)

                    cache.evict(key)

                    cache.get(key).shouldBeNull()
                }
            }

            context("ttl") {
                test("value expires") {
                    val key = url("ttl")
                    cache.put(key, mediaFor(key), EXPIRING_TTL)

                    cache.get(key)!!.shouldHaveSize(2)

                    eventually(3.seconds) {
                        cache.get(key).shouldBeNull()
                    }
                }
            }

            context("schema versioning") {
                test("different schemaVersion => separate keyspace") {
                    val key = url("ver")

                    val v1: CachePort<String, List<Media>> = AsyncRedisMediaCacheAdapter(redisTemplate, schemaVersion = 1)
                    val v2: CachePort<String, List<Media>> = AsyncRedisMediaCacheAdapter(redisTemplate, schemaVersion = 2)

                    v1.put(key, mediaFor(key), DEFAULT_TTL)

                    v1.get(key)!!.shouldHaveSize(2)
                    v2.get(key).shouldBeNull()
                }
            }
        }) {
        private companion object {
            val FIXED_DOWNLOADED_AT: OffsetDateTime = OffsetDateTime.parse("2020-01-01T00:00:00Z")

            val DEFAULT_TTL: Duration = Duration.ofSeconds(30)
            val EXPIRING_TTL: Duration = Duration.ofMillis(250)
        }
    }

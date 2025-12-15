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
class AsyncRedisMediaCacheAdapterIT @Autowired constructor(
    private val cache: CachePort<String, List<Media>>,
    private val mediaTemplate: ReactiveRedisTemplate<String, List<Media>>,
    private val connectionFactory: ReactiveRedisConnectionFactory,
) : FunSpec({

    extension(SpringExtension)

    suspend fun flushAll() {
        connectionFactory.reactiveConnection
            .serverCommands()
            .flushAll()
            .awaitFirstOrNull()
    }

    val fixedDownloadedAt = OffsetDateTime.parse("2020-01-01T00:00:00Z")

    fun media(url: String) =
        listOf(
            Media(
                type = MediaType.VIDEO,
                fileUrl = "file:///tmp/video.mp4",
                sourceUrl = url,
                title = "Video",
                fileUniqueId = "uniq-1",
                lastFileId = "file-1",
                downloadedAt = fixedDownloadedAt,
            ),
            Media(
                type = MediaType.IMAGE,
                fileUrl = "file:///tmp/1.jpg",
                sourceUrl = url,
                title = "Img",
                fileUniqueId = "uniq-2",
                lastFileId = "file-2",
                downloadedAt = fixedDownloadedAt,
            ),
        )

    beforeTest {
        flushAll()
    }

    test("get: returns null when key is absent") {
        cache.get("https://example.com/miss").shouldBeNull()
    }

    test("put/get: stores and reads list back") {
        val url = "https://example.com/hit"
        val value = media(url)

        cache.put(url, value, ttl = Duration.ofSeconds(30))

        val fromCache = cache.get(url)
        fromCache shouldBe value
        fromCache!!.shouldHaveSize(2)
    }

    test("put: does nothing when values are empty") {
        val url = "https://example.com/empty"

        cache.put(url, emptyList(), ttl = Duration.ofSeconds(30))

        cache.get(url).shouldBeNull()
    }

    test("evict: removes key") {
        val url = "https://example.com/evict"
        cache.put(url, media(url), ttl = Duration.ofSeconds(30))

        cache.get(url)!!.shouldHaveSize(2)

        cache.evict(url)

        cache.get(url).shouldBeNull()
    }

    test("ttl: value expires") {
        val url = "https://example.com/ttl"
        cache.put(url, media(url), ttl = Duration.ofMillis(150))

        cache.get(url)!!.shouldHaveSize(2)

        eventually(2.seconds) {
            cache.get(url).shouldBeNull()
        }
    }

    test("schema versioning: different schemaVersion => separate keyspace") {
        val url = "https://example.com/ver"
        val v1 = AsyncRedisMediaCacheAdapter(mediaTemplate, schemaVersion = 1)
        val v2 = AsyncRedisMediaCacheAdapter(mediaTemplate, schemaVersion = 2)

        v1.put(url, media(url), ttl = Duration.ofSeconds(30))

        v1.get(url)!!.shouldHaveSize(2)
        v2.get(url).shouldBeNull()
    }
})


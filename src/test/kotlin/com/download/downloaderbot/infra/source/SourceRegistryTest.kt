package com.download.downloaderbot.infra.source

import com.download.downloaderbot.app.config.properties.SourceDef
import com.download.downloaderbot.app.config.properties.SourcesProperties
import com.download.downloaderbot.app.config.properties.SubresourceDef
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

class SourceRegistryTest : FunSpec({

    test("match returns null when there are no sources") {
        val props = SourcesProperties(emptyMap())

        val registry = SourceRegistry(props)

        registry.match("https://example.com").shouldBeNull()
    }

    test("match returns null when no patterns match given url") {
        val props = SourcesProperties(
            sources = linkedMapOf(
                "youtube" to sourceDef(true,
                    sub(
                        name = "videos",
                        tool = "yt-dlp",
                        format = "mp4",
                        patterns = listOf("youtube\\.com/watch"),
                    ),
                ),
            ),
        )

        val registry = SourceRegistry(props)

        registry.match("https://vimeo.com/123").shouldBeNull()
    }

    test("match returns first matching source and subresource") {
        val props = SourcesProperties(
            sources = linkedMapOf(
                "youtube" to sourceDef(true,
                    sub(
                        name = "videos",
                        tool = "yt-dlp",
                        format = "mp4",
                        patterns = listOf("youtube\\.com/watch"),
                    ),
                    sub(
                        name = "shorts",
                        tool = "yt-dlp",
                        format = "mp4",
                        patterns = listOf("youtube\\.com/shorts"),
                    ),
                ),
                "tiktok" to sourceDef(true,
                    sub(
                        name = "posts",
                        tool = "tiktok-tool",
                        format = "mp4",
                        patterns = listOf("tiktok\\.com/@"),
                    ),
                ),
            ),
        )

        val registry = SourceRegistry(props)

        val videoUrl = "https://www.youtube.com/watch?v=abc"
        val shortsUrl = "https://www.youtube.com/shorts/xyz"
        val tiktokUrl = "https://www.tiktok.com/@user/video/123"

        val videoMatch = registry.match(videoUrl)
        videoMatch.shouldNotBeNull()
        videoMatch shouldBe SourceMatch(
            source = "youtube",
            subresource = "videos",
            tool = "yt-dlp",
            format = "mp4",
        )

        val shortsMatch = registry.match(shortsUrl)
        shortsMatch.shouldNotBeNull()
        shortsMatch shouldBe SourceMatch(
            source = "youtube",
            subresource = "shorts",
            tool = "yt-dlp",
            format = "mp4",
        )

        val tiktokMatch = registry.match(tiktokUrl)
        tiktokMatch.shouldNotBeNull()
        tiktokMatch shouldBe SourceMatch(
            source = "tiktok",
            subresource = "posts",
            tool = "tiktok-tool",
            format = "mp4",
        )
    }

    test("match prefers first matching subresource when multiple patterns match") {
        val props = SourcesProperties(
            sources = linkedMapOf(
                "example" to sourceDef(true,
                    sub(
                        name = "generic",
                        tool = "generic-tool",
                        format = "raw",
                        patterns = listOf("example\\.com"),
                    ),
                    sub(
                        name = "specific",
                        tool = "specific-tool",
                        format = "json",
                        patterns = listOf("example\\.com/path"),
                    ),
                ),
            ),
        )

        val registry = SourceRegistry(props)

        val url = "https://example.com/path/to/resource"

        val match = registry.match(url)
        match.shouldNotBeNull()
        match shouldBe SourceMatch(
            source = "example",
            subresource = "generic",
            tool = "generic-tool",
            format = "raw",
        )
    }

    test("disabled sources are ignored") {
        val pattern = "example\\.com"

        val props = SourcesProperties(
            sources = linkedMapOf(
                "disabled" to sourceDef(
                    enabled = false,
                    sub(
                        name = "any",
                        tool = "should-not-be-used",
                        format = "",
                        patterns = listOf(pattern),
                    ),
                ),
                "enabled" to sourceDef(
                    enabled = true,
                    sub(
                        name = "resource",
                        tool = "real-tool",
                        format = "mp4",
                        patterns = listOf(pattern),
                    ),
                ),
            ),
        )

        val registry = SourceRegistry(props)

        val match = registry.match("https://example.com/video/42")
        match.shouldNotBeNull()
        match shouldBe SourceMatch(
            source = "enabled",
            subresource = "resource",
            tool = "real-tool",
            format = "mp4",
        )
    }

    test("disabled subresources are ignored") {
        val pattern = "example\\.com"

        val props = SourcesProperties(
            sources = linkedMapOf(
                "source" to sourceDef(true,
                    sub(
                        name = "disabledSub",
                        enabled = false,
                        tool = "disabled-tool",
                        format = "xml",
                        patterns = listOf(pattern),
                    ),
                    sub(
                        name = "enabledSub",
                        enabled = true,
                        tool = "enabled-tool",
                        format = "json",
                        patterns = listOf(pattern),
                    ),
                ),
            ),
        )

        val registry = SourceRegistry(props)

        val match = registry.match("https://example.com/path")
        match.shouldNotBeNull()
        match shouldBe SourceMatch(
            source = "source",
            subresource = "enabledSub",
            tool = "enabled-tool",
            format = "json",
        )
    }

    test("list returns compiled representation with patterns compiled") {
        val props = SourcesProperties(
            sources = linkedMapOf(
                "youtube" to sourceDef(true,
                    sub(
                        name = "videos",
                        tool = "yt-dlp",
                        format = "mp4",
                        patterns = listOf("youtube\\.com/watch", "youtu\\.be/"),
                    ),
                ),
            ),
        )

        val registry = SourceRegistry(props)

        val compiled = registry.list()

        compiled.shouldHaveSize(1)
        val only = compiled.single()
        only.name shouldBe "youtube"

        val sub = only.subresources["videos"]
            ?: error("Expected 'videos' subresource in compiled source")

        sub.tool shouldBe "yt-dlp"
        sub.format shouldBe "mp4"
        sub.patterns.map { it.pattern() } shouldContainExactly listOf(
            "youtube\\.com/watch",
            "youtu\\.be/",
        )
    }

    test("reload updates compiled sources and match uses new configuration") {
        val initialProps = SourcesProperties(
            sources = linkedMapOf(
                "youtube" to sourceDef(true,
                    sub(
                        name = "videos",
                        tool = "yt-dlp",
                        format = "mp4",
                        patterns = listOf("youtube\\.com/watch"),
                    ),
                ),
            ),
        )

        val registry = SourceRegistry(initialProps)

        val youtubeUrl = "https://www.youtube.com/watch?v=abc"
        registry.match(youtubeUrl)
            .shouldNotBeNull()
            .source shouldBe "youtube"

        val newProps = SourcesProperties(
            sources = linkedMapOf(
                "tiktok" to sourceDef(true,
                    sub(
                        name = "posts",
                        tool = "tiktok-tool",
                        format = "mp4",
                        patterns = listOf("tiktok\\.com/@"),
                    ),
                ),
            ),
        )

        registry.reload(newProps)

        registry.match(youtubeUrl).shouldBeNull()

        val tiktokUrl = "https://www.tiktok.com/@user/video/123"
        val newMatch = registry.match(tiktokUrl)
        newMatch.shouldNotBeNull()
        newMatch shouldBe SourceMatch(
            source = "tiktok",
            subresource = "posts",
            tool = "tiktok-tool",
            format = "mp4",
        )
    }
})

private fun sourceDef(
    enabled: Boolean = true,
    vararg subs: SubEntry,
): SourceDef = SourceDef(
    enabled = enabled,
    subresources = linkedMapOf(*subs.map { it.name to it.def }.toTypedArray()),
)

private data class SubEntry(
    val name: String,
    val def: SubresourceDef,
)

private fun sub(
    name: String,
    tool: String,
    format: String = "",
    enabled: Boolean = true,
    patterns: List<String>,
): SubEntry = SubEntry(
    name = name,
    def = SubresourceDef(
        enabled = enabled,
        tool = tool,
        format = format,
        urlPatterns = patterns,
    ),
)

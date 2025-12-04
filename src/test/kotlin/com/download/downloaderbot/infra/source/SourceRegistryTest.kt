package com.download.downloaderbot.infra.source

import com.download.downloaderbot.app.config.properties.SourceDef
import com.download.downloaderbot.app.config.properties.SourcesProperties
import com.download.downloaderbot.app.config.properties.SubresourceDef
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.nulls.shouldNotBeNull

class SourceRegistryTest : FunSpec({

    context("match") {

        test("returns null when no sources configured") {
            val registry = registry { }

            registry.match("https://example.com").shouldBeNull()
        }

        test("returns null when no patterns match url") {
            val registry = registry {
                source("youtube") {
                    sub(
                        name = "videos",
                        tool = "yt-dlp",
                        format = "mp4",
                        patterns = listOf("youtube\\.com/watch"),
                    )
                }
            }

            registry.match("https://vimeo.com/123").shouldBeNull()
        }

        test("matches youtube videos, youtube shorts and tiktok posts") {
            val registry = registry {
                source("youtube") {
                    sub(
                        name = "videos",
                        tool = "yt-dlp",
                        format = "mp4",
                        patterns = listOf("youtube\\.com/watch"),
                    )
                    sub(
                        name = "shorts",
                        tool = "yt-dlp",
                        format = "mp4",
                        patterns = listOf("youtube\\.com/shorts"),
                    )
                }
                source("tiktok") {
                    sub(
                        name = "posts",
                        tool = "tiktok-tool",
                        format = "mp4",
                        patterns = listOf("tiktok\\.com/@"),
                    )
                }
            }

            val videoUrl = "https://www.youtube.com/watch?v=abc"
            val shortsUrl = "https://www.youtube.com/shorts/xyz"
            val tiktokUrl = "https://www.tiktok.com/@user/video/123"

            registry.requireMatch(videoUrl) shouldBe SourceMatch(
                source = "youtube",
                subresource = "videos",
                tool = "yt-dlp",
                format = "mp4",
            )

            registry.requireMatch(shortsUrl) shouldBe SourceMatch(
                source = "youtube",
                subresource = "shorts",
                tool = "yt-dlp",
                format = "mp4",
            )

            registry.requireMatch(tiktokUrl) shouldBe SourceMatch(
                source = "tiktok",
                subresource = "posts",
                tool = "tiktok-tool",
                format = "mp4",
            )
        }

        test("prefers first matching subresource when multiple patterns match") {
            val registry = registry {
                source("example") {
                    sub(
                        name = "generic",
                        tool = "generic-tool",
                        format = "raw",
                        patterns = listOf("example\\.com"),
                    )
                    sub(
                        name = "specific",
                        tool = "specific-tool",
                        format = "json",
                        patterns = listOf("example\\.com/path"),
                    )
                }
            }

            val url = "https://example.com/path/to/resource"

            registry.requireMatch(url) shouldBe SourceMatch(
                source = "example",
                subresource = "generic", // порядок subresources важен
                tool = "generic-tool",
                format = "raw",
            )
        }

        test("ignores disabled sources") {
            val pattern = "example\\.com"

            val registry = registry {
                source(
                    name = "disabled",
                    enabled = false,
                ) {
                    sub(
                        name = "any",
                        tool = "should-not-be-used",
                        format = "",
                        patterns = listOf(pattern),
                    )
                }
                source("enabled") {
                    sub(
                        name = "resource",
                        tool = "real-tool",
                        format = "mp4",
                        patterns = listOf(pattern),
                    )
                }
            }

            registry.requireMatch("https://example.com/video/42") shouldBe SourceMatch(
                source = "enabled",
                subresource = "resource",
                tool = "real-tool",
                format = "mp4",
            )
        }

        test("ignores disabled subresources") {
            val pattern = "example\\.com"

            val registry = registry {
                source("source") {
                    sub(
                        name = "disabledSub",
                        tool = "disabled-tool",
                        format = "xml",
                        enabled = false,
                        patterns = listOf(pattern),
                    )
                    sub(
                        name = "enabledSub",
                        tool = "enabled-tool",
                        format = "json",
                        patterns = listOf(pattern),
                    )
                }
            }

            registry.requireMatch("https://example.com/path") shouldBe SourceMatch(
                source = "source",
                subresource = "enabledSub",
                tool = "enabled-tool",
                format = "json",
            )
        }
    }

    context("list") {

        test("returns compiled representation with compiled patterns") {
            val registry = registry {
                source("youtube") {
                    sub(
                        name = "videos",
                        tool = "yt-dlp",
                        format = "mp4",
                        patterns = listOf("youtube\\.com/watch", "youtu\\.be/"),
                    )
                }
            }

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
    }

    context("reload") {

        test("reload replaces configuration used by match") {
            val registry = registry {
                source("youtube") {
                    sub(
                        name = "videos",
                        tool = "yt-dlp",
                        format = "mp4",
                        patterns = listOf("youtube\\.com/watch"),
                    )
                }
            }

            val youtubeUrl = "https://www.youtube.com/watch?v=abc"

            registry.requireMatch(youtubeUrl).source shouldBe "youtube"

            val newProps = SourcesProperties(
                sources = linkedMapOf(
                    "tiktok" to SourceDef(
                        enabled = true,
                        subresources = linkedMapOf(
                            "posts" to SubresourceDef(
                                enabled = true,
                                tool = "tiktok-tool",
                                format = "mp4",
                                urlPatterns = listOf("tiktok\\.com/@"),
                            ),
                        ),
                    ),
                ),
            )

            registry.reload(newProps)

            registry.match(youtubeUrl).shouldBeNull()

            val tiktokUrl = "https://www.tiktok.com/@user/video/123"
            registry.requireMatch(tiktokUrl) shouldBe SourceMatch(
                source = "tiktok",
                subresource = "posts",
                tool = "tiktok-tool",
                format = "mp4",
            )
        }
    }
})

private fun registry(block: SourcesBuilder.() -> Unit): SourceRegistry {
    val sources = SourcesBuilder().apply(block).build()
    return SourceRegistry(SourcesProperties(sources))
}

private class SourcesBuilder {
    private val sources = linkedMapOf<String, SourceDef>()

    fun source(
        name: String,
        enabled: Boolean = true,
        block: SubresourcesBuilder.() -> Unit,
    ) {
        val subs = SubresourcesBuilder().apply(block).build()
        sources[name] = SourceDef(
            enabled = enabled,
            subresources = subs,
        )
    }

    fun build(): Map<String, SourceDef> = sources
}

private class SubresourcesBuilder {
    private val subs = linkedMapOf<String, SubresourceDef>()

    fun sub(
        name: String,
        tool: String,
        format: String = "",
        enabled: Boolean = true,
        patterns: List<String>,
    ) {
        subs[name] = SubresourceDef(
            enabled = enabled,
            tool = tool,
            format = format,
            urlPatterns = patterns,
        )
    }

    fun build(): Map<String, SubresourceDef> = subs
}

private fun SourceRegistry.requireMatch(url: String): SourceMatch =
    match(url).also { it.shouldNotBeNull() }!!

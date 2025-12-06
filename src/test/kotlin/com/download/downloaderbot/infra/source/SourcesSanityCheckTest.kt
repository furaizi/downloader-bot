package com.download.downloaderbot.infra.source

import com.download.downloaderbot.app.config.properties.SourcesProperties
import com.download.downloaderbot.infra.process.cli.api.ToolRegistry
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class SourcesSanityCheckTest : FunSpec({

    fun sourcesWithTools(vararg tools: String): SourcesProperties =
        sourcesProps {
            source("test-source") {
                tools.forEach { toolName ->
                    sub(
                        name = toolName,
                        tool = toolName,
                        patterns = listOf("https://example.com/$toolName/.*"),
                    )
                }
            }
        }

    test("check passes when all enabled tools exist in ToolRegistry") {
        val props = sourcesWithTools("yt-dlp", "gallery-dl")

        val toolRegistry = mockk<ToolRegistry>()
        every { toolRegistry.get("yt-dlp") } returns mockk(relaxed = true)
        every { toolRegistry.get("gallery-dl") } returns mockk(relaxed = true)

        val sourceRegistry = SourceRegistry(props)
        val sanityCheck = SourcesSanityCheck(sourceRegistry, toolRegistry, props)

        shouldNotThrowAny {
            sanityCheck.check()
        }

        sourceRegistry.list().shouldNotBeEmpty()
    }

    test("check fails when at least one enabled tool is missing in ToolRegistry") {
        val props = sourcesWithTools("yt-dlp", "missing-tool")

        val toolRegistry = mockk<ToolRegistry>()
        every { toolRegistry.get("yt-dlp") } returns mockk(relaxed = true)
        every { toolRegistry.get("missing-tool") } throws IllegalStateException("Tool not found: missing-tool")

        val sourceRegistry = SourceRegistry(props)
        val sanityCheck = SourcesSanityCheck(sourceRegistry, toolRegistry, props)

        val ex = shouldThrow<IllegalArgumentException> {
            sanityCheck.check()
        }

        ex.message.shouldContain("missing-tool")
        ex.message.shouldContain("Unknown tools in sources.yml")
    }

    test("check ignores disabled sources and subresources when validating tools") {
        val props =
            sourcesProps {
                source("enabled-source") {
                    sub(
                        name = "active",
                        tool = "known-tool",
                        patterns = listOf("https://example.com/.*"),
                    )
                    sub(
                        name = "disabled-sub",
                        tool = "missing-tool",
                        enabled = false,
                        patterns = listOf("https://example.com/.*"),
                    )
                }
                source(
                    name = "disabled-source",
                    enabled = false,
                ) {
                    sub(
                        name = "ignored",
                        tool = "missing-tool-2",
                        patterns = listOf("https://example.com/.*"),
                    )
                }
            }

        val toolRegistry = mockk<ToolRegistry>()
        every { toolRegistry.get("known-tool") } returns mockk(relaxed = true)

        val sourceRegistry = SourceRegistry(props)
        val sanityCheck = SourcesSanityCheck(sourceRegistry, toolRegistry, props)

        shouldNotThrowAny {
            sanityCheck.check()
        }

        verify(exactly = 1) { toolRegistry.get("known-tool") }
        verify(exactly = 0) { toolRegistry.get("missing-tool") }
        verify(exactly = 0) { toolRegistry.get("missing-tool-2") }
    }
})

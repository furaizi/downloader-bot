package com.download.downloaderbot.infra.media.provider

import com.download.downloaderbot.core.domain.Media
import com.download.downloaderbot.core.downloader.UnsupportedSourceException
import com.download.downloaderbot.infra.process.cli.api.CliTool
import com.download.downloaderbot.infra.process.cli.api.ToolRegistry
import com.download.downloaderbot.infra.source.SourceMatch
import com.download.downloaderbot.infra.source.SourceRegistry
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class DynamicMediaProviderTest : FunSpec({

    val sources = mockk<SourceRegistry>()
    val tools = mockk<ToolRegistry>()
    val provider = DynamicMediaProvider(sources, tools)

    context("supports") {

        test("returns false when SourceRegistry has no match") {
            every { sources.match(any()) } returns null

            val supported = provider.supports("https://example.com/unknown")

            supported.shouldBeFalse()
            verify { sources.match("https://example.com/unknown") }
        }

        test("returns true when SourceRegistry has match") {
            val url = "https://www.youtube.com/watch?v=abc"
            every { sources.match(url) } returns
                SourceMatch(
                    source = "youtube",
                    subresource = "videos",
                    tool = "yt-dlp",
                    format = "mp4",
                )

            val supported = provider.supports(url)

            supported.shouldBeTrue()
            verify { sources.match(url) }
        }
    }

    context("download") {

        test("throws UnsupportedSourceException when SourceRegistry has no match") {
            val url = "https://example.com/unsupported"
            every { sources.match(url) } returns null

            shouldThrow<UnsupportedSourceException> {
                provider.download(url)
            }

            verify { sources.match(url) }
        }

        test("delegates to matched tool with provided format and returns its result") {
            val url = "https://www.youtube.com/watch?v=abc"
            val match =
                SourceMatch(
                    source = "youtube",
                    subresource = "videos",
                    tool = "yt-dlp",
                    format = "mp4",
                )

            every { sources.match(url) } returns match

            val cliTool = mockk<CliTool>()
            every { tools.get("yt-dlp") } returns cliTool

            val expectedMedia =
                listOf(
                    mockk<Media>(),
                    mockk<Media>(),
                )

            coEvery { cliTool.download(url, "mp4") } returns expectedMedia

            val result = provider.download(url)

            result shouldBe expectedMedia

            verify { sources.match(url) }
            verify { tools.get("yt-dlp") }
            coVerify { cliTool.download(url, "mp4") }
        }

        test("passes empty format when match.format is blank") {
            val url = "https://example.com/resource"
            val match =
                SourceMatch(
                    source = "example",
                    subresource = "default",
                    tool = "generic-tool",
                    format = "",
                )

            every { sources.match(url) } returns match

            val cliTool = mockk<CliTool>()
            every { tools.get("generic-tool") } returns cliTool

            val expectedMedia =
                listOf(
                    mockk<Media>(),
                )

            coEvery { cliTool.download(url, "") } returns expectedMedia

            val result = provider.download(url)

            result shouldBe expectedMedia

            verify { sources.match(url) }
            verify { tools.get("generic-tool") }
            coVerify { cliTool.download(url, "") }
        }
    }
})

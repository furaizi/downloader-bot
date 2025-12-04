package com.download.downloaderbot.infra.media.provider

import com.download.downloaderbot.app.config.properties.SourceDef
import com.download.downloaderbot.app.config.properties.SourcesProperties
import com.download.downloaderbot.app.config.properties.SubresourceDef
import com.download.downloaderbot.core.domain.Media
import com.download.downloaderbot.core.downloader.UnsupportedSourceException
import com.download.downloaderbot.infra.process.cli.api.CliTool
import com.download.downloaderbot.infra.process.cli.api.ToolId
import com.download.downloaderbot.infra.process.cli.api.ToolRegistry
import com.download.downloaderbot.infra.source.SourceRegistry
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class DynamicMediaProviderTest {
    @Test
    fun `supports returns true when source matches`() =
        runTest {
            val provider =
                createProvider(
                    sources = mapOf("youtube" to sourceDef("video" to subresourceDef("yt-dlp", ".*youtube.*"))),
                )

            assertTrue(provider.supports("https://youtube.com/watch?v=abc"))
        }

    @Test
    fun `supports returns false when no source matches`() =
        runTest {
            val provider =
                createProvider(
                    sources = mapOf("youtube" to sourceDef("video" to subresourceDef("yt-dlp", ".*youtube.*"))),
                )

            assertFalse(provider.supports("https://vimeo.com/123"))
        }

    @Test
    fun `download uses correct tool from match`() =
        runTest {
            val expectedMedia = listOf(mockk<Media>())
            val ytDlpTool =
                mockk<CliTool> {
                    every { toolId } returns ToolId.YT_DLP
                    coEvery { download(any(), any()) } returns expectedMedia
                }

            val provider =
                createProvider(
                    sources = mapOf("youtube" to sourceDef("video" to subresourceDef("yt-dlp", ".*youtube.*"))),
                    tools = listOf(ytDlpTool),
                )

            val result = provider.download("https://youtube.com/watch?v=abc")

            assertEquals(expectedMedia, result)
            coVerify { ytDlpTool.download("https://youtube.com/watch?v=abc", "") }
        }

    @Test
    fun `download passes format to tool`() =
        runTest {
            val ytDlpTool =
                mockk<CliTool> {
                    every { toolId } returns ToolId.YT_DLP
                    coEvery { download(any(), any()) } returns emptyList()
                }

            val provider =
                createProvider(
                    sources =
                        mapOf(
                            "youtube" to
                                sourceDef(
                                    "video" to subresourceDef("yt-dlp", ".*youtube.*", format = "bestvideo"),
                                ),
                        ),
                    tools = listOf(ytDlpTool),
                )

            provider.download("https://youtube.com/watch?v=abc")

            coVerify { ytDlpTool.download("https://youtube.com/watch?v=abc", "bestvideo") }
        }

    @Test
    fun `download throws UnsupportedSourceException when no match`() =
        runTest {
            val provider =
                createProvider(
                    sources = mapOf("youtube" to sourceDef("video" to subresourceDef("yt-dlp", ".*youtube.*"))),
                )

            assertThrows<UnsupportedSourceException> {
                provider.download("https://unknown-site.com/video")
            }
        }

    @Test
    fun `routes to different tools based on source`() =
        runTest {
            val ytMedia = listOf(mockk<Media>())
            val galleryMedia = listOf(mockk<Media>(), mockk<Media>())

            val ytDlpTool =
                mockk<CliTool> {
                    every { toolId } returns ToolId.YT_DLP
                    coEvery { download(any(), any()) } returns ytMedia
                }
            val galleryDlTool =
                mockk<CliTool> {
                    every { toolId } returns ToolId.GALLERY_DL
                    coEvery { download(any(), any()) } returns galleryMedia
                }

            val provider =
                createProvider(
                    sources =
                        mapOf(
                            "youtube" to sourceDef("video" to subresourceDef("yt-dlp", ".*youtube.*")),
                            "tiktok" to sourceDef("video" to subresourceDef("gallery-dl", ".*tiktok.*")),
                        ),
                    tools = listOf(ytDlpTool, galleryDlTool),
                )

            assertEquals(ytMedia, provider.download("https://youtube.com/watch"))
            assertEquals(galleryMedia, provider.download("https://tiktok.com/@user/video/123"))
        }

    private fun createProvider(
        sources: Map<String, SourceDef>,
        tools: List<CliTool> = listOf(createMockTool(ToolId.YT_DLP)),
    ): DynamicMediaProvider {
        val sourceRegistry = SourceRegistry(SourcesProperties(sources))
        val toolRegistry = ToolRegistry(tools)
        return DynamicMediaProvider(sourceRegistry, toolRegistry)
    }

    private fun createMockTool(id: ToolId): CliTool =
        mockk {
            every { toolId } returns id
            coEvery { download(any(), any()) } returns emptyList()
        }

    private fun sourceDef(vararg subresources: Pair<String, SubresourceDef>): SourceDef =
        SourceDef(enabled = true, subresources = subresources.toMap())

    private fun subresourceDef(
        tool: String,
        vararg patterns: String,
        format: String = "",
    ): SubresourceDef = SubresourceDef(enabled = true, tool = tool, format = format, urlPatterns = patterns.toList())
}

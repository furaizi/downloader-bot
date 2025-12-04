package com.download.downloaderbot.infra.source

import com.download.downloaderbot.app.config.properties.SourceDef
import com.download.downloaderbot.app.config.properties.SourcesProperties
import com.download.downloaderbot.app.config.properties.SubresourceDef
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class SourceRegistryTest {
    @Test
    fun `matches url to correct source and subresource`() {
        val registry =
            createRegistry(
                "youtube" to
                    sourceDef(
                        subresources = mapOf("video" to subresourceDef("yt-dlp", ".*youtube\\.com/watch.*")),
                    ),
            )

        val match = registry.match("https://www.youtube.com/watch?v=abc")

        assertNotNull(match)
        assertEquals("youtube", match!!.source)
        assertEquals("video", match.subresource)
        assertEquals("yt-dlp", match.tool)
    }

    @Test
    fun `returns null when no pattern matches`() {
        val registry =
            createRegistry(
                "youtube" to
                    sourceDef(
                        subresources = mapOf("video" to subresourceDef("yt-dlp", ".*youtube\\.com/watch.*")),
                    ),
            )

        val match = registry.match("https://vimeo.com/123456")

        assertNull(match)
    }

    @Test
    fun `skips disabled sources`() {
        val registry =
            createRegistry(
                "youtube" to
                    sourceDef(
                        enabled = false,
                        subresources = mapOf("video" to subresourceDef("yt-dlp", ".*youtube\\.com/watch.*")),
                    ),
            )

        val match = registry.match("https://www.youtube.com/watch?v=abc")

        assertNull(match)
    }

    @Test
    fun `skips disabled subresources`() {
        val registry =
            createRegistry(
                "youtube" to
                    sourceDef(
                        subresources =
                            mapOf(
                                "video" to subresourceDef("yt-dlp", ".*youtube\\.com/watch.*", enabled = false),
                                "shorts" to subresourceDef("yt-dlp", ".*youtube\\.com/shorts.*"),
                            ),
                    ),
            )

        assertNull(registry.match("https://www.youtube.com/watch?v=abc"))
        assertNotNull(registry.match("https://www.youtube.com/shorts/xyz"))
    }

    @Test
    fun `includes format in match result`() {
        val registry =
            createRegistry(
                "youtube" to
                    sourceDef(
                        subresources =
                            mapOf(
                                "video" to
                                    subresourceDef(
                                        tool = "yt-dlp",
                                        pattern = ".*youtube\\.com/watch.*",
                                        format = "bestvideo+bestaudio",
                                    ),
                            ),
                    ),
            )

        val match = registry.match("https://www.youtube.com/watch?v=abc")

        assertEquals("bestvideo+bestaudio", match!!.format)
    }

    @Test
    fun `list returns all compiled sources`() {
        val registry =
            createRegistry(
                "youtube" to sourceDef(subresources = mapOf("video" to subresourceDef("yt-dlp", ".*youtube.*"))),
                "tiktok" to sourceDef(subresources = mapOf("video" to subresourceDef("gallery-dl", ".*tiktok.*"))),
            )

        val sources = registry.list()

        assertEquals(2, sources.size)
        assertEquals(setOf("youtube", "tiktok"), sources.map { it.name }.toSet())
    }

    @Test
    fun `reload updates patterns`() {
        val registry =
            createRegistry(
                "youtube" to sourceDef(subresources = mapOf("video" to subresourceDef("yt-dlp", ".*youtube.*"))),
            )

        assertNotNull(registry.match("https://youtube.com/watch"))
        assertNull(registry.match("https://vimeo.com/123"))

        registry.reload(
            SourcesProperties(
                sources =
                    mapOf(
                        "vimeo" to sourceDef(subresources = mapOf("video" to subresourceDef("yt-dlp", ".*vimeo.*"))),
                    ),
            ),
        )

        assertNull(registry.match("https://youtube.com/watch"))
        assertNotNull(registry.match("https://vimeo.com/123"))
    }

    @Test
    fun `matches first matching subresource`() {
        val registry =
            createRegistry(
                "youtube" to
                    sourceDef(
                        subresources =
                            linkedMapOf(
                                "shorts" to subresourceDef("yt-dlp", ".*youtube\\.com/shorts.*"),
                                "video" to subresourceDef("yt-dlp", ".*youtube\\.com.*"),
                            ),
                    ),
            )

        val shortsMatch = registry.match("https://www.youtube.com/shorts/abc")
        assertEquals("shorts", shortsMatch!!.subresource)

        val videoMatch = registry.match("https://www.youtube.com/watch?v=abc")
        assertEquals("video", videoMatch!!.subresource)
    }

    private fun createRegistry(vararg sources: Pair<String, SourceDef>): SourceRegistry {
        val props = SourcesProperties(sources = sources.toMap())
        return SourceRegistry(props)
    }

    private fun sourceDef(
        subresources: Map<String, SubresourceDef>,
        enabled: Boolean = true,
    ): SourceDef = SourceDef(enabled = enabled, subresources = subresources)

    private fun subresourceDef(
        tool: String,
        pattern: String,
        format: String = "",
        enabled: Boolean = true,
    ): SubresourceDef =
        SubresourceDef(
            enabled = enabled,
            tool = tool,
            format = format,
            urlPatterns = listOf(pattern),
        )
}

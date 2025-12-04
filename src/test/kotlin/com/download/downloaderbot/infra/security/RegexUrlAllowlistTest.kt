package com.download.downloaderbot.infra.security

import com.download.downloaderbot.app.config.properties.SourceAllowProperties
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RegexUrlAllowlistTest {
    @Test
    fun `allows url matching single pattern`() {
        val allowlist = createAllowlist(".*youtube\\.com.*")

        assertTrue(allowlist.isAllowed("https://www.youtube.com/watch?v=abc"))
    }

    @Test
    fun `allows url matching any of multiple patterns`() {
        val allowlist = createAllowlist(".*youtube\\.com.*", ".*vimeo\\.com.*")

        assertTrue(allowlist.isAllowed("https://www.youtube.com/watch?v=abc"))
        assertTrue(allowlist.isAllowed("https://vimeo.com/123456"))
    }

    @Test
    fun `rejects url not matching any pattern`() {
        val allowlist = createAllowlist(".*youtube\\.com.*")

        assertFalse(allowlist.isAllowed("https://evil.com/video"))
    }

    @Test
    fun `empty allowlist rejects all urls`() {
        val allowlist = createAllowlist()

        assertFalse(allowlist.isAllowed("https://youtube.com"))
        assertFalse(allowlist.isAllowed("https://example.com"))
    }

    @Test
    fun `patterns are case insensitive`() {
        val allowlist = createAllowlist(".*youtube\\.com.*")

        assertTrue(allowlist.isAllowed("https://YOUTUBE.COM/watch"))
        assertTrue(allowlist.isAllowed("https://YouTube.com/watch"))
    }

    @Test
    fun `pattern matches partial url`() {
        val allowlist = createAllowlist("tiktok")

        assertTrue(allowlist.isAllowed("https://www.tiktok.com/@user/video/123"))
    }

    private fun createAllowlist(vararg patterns: String): RegexUrlAllowlist {
        val props = SourceAllowProperties(allow = patterns.toList())
        return RegexUrlAllowlist(props)
    }
}

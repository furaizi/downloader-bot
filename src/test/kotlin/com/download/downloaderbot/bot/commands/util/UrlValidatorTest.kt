package com.download.downloaderbot.bot.commands.util

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class UrlValidatorTest {
    private val validator = UrlValidator()

    @Test
    fun `valid https url returns true`() {
        assertTrue(validator.isHttpUrl("https://example.com"))
    }

    @Test
    fun `valid http url returns true`() {
        assertTrue(validator.isHttpUrl("http://example.com"))
    }

    @Test
    fun `url with path returns true`() {
        assertTrue(validator.isHttpUrl("https://example.com/path/to/resource"))
    }

    @Test
    fun `url with query params returns true`() {
        assertTrue(validator.isHttpUrl("https://example.com?foo=bar&baz=qux"))
    }

    @Test
    fun `blank string returns false`() {
        assertFalse(validator.isHttpUrl(""))
        assertFalse(validator.isHttpUrl("   "))
    }

    @Test
    fun `ftp scheme returns false`() {
        assertFalse(validator.isHttpUrl("ftp://example.com"))
    }

    @Test
    fun `mailto scheme returns false`() {
        assertFalse(validator.isHttpUrl("mailto:user@example.com"))
    }

    @Test
    fun `missing scheme returns false`() {
        assertFalse(validator.isHttpUrl("example.com"))
    }

    @Test
    fun `missing host returns false`() {
        assertFalse(validator.isHttpUrl("https://"))
    }

    @Test
    fun `invalid url returns false`() {
        assertFalse(validator.isHttpUrl("not a url at all"))
    }
}

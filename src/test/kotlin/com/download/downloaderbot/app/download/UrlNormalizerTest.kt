package com.download.downloaderbot.app.download

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class UrlNormalizerTest {
    private val normalizer = UrlNormalizer()

    @Nested
    inner class SchemeNormalization {
        @Test
        fun `lowercases scheme`() {
            assertEquals("https://example.com/", normalizer.normalize("HTTPS://example.com"))
        }

        @Test
        fun `preserves http scheme`() {
            assertEquals("http://example.com/", normalizer.normalize("http://example.com"))
        }
    }

    @Nested
    inner class HostNormalization {
        @Test
        fun `lowercases host`() {
            assertEquals("https://example.com/", normalizer.normalize("https://EXAMPLE.COM"))
        }
    }

    @Nested
    inner class PortNormalization {
        @Test
        fun `removes default https port 443`() {
            assertEquals("https://example.com/", normalizer.normalize("https://example.com:443"))
        }

        @Test
        fun `removes default http port 80`() {
            assertEquals("http://example.com/", normalizer.normalize("http://example.com:80"))
        }

        @Test
        fun `preserves non-default port`() {
            assertEquals("https://example.com:8080/", normalizer.normalize("https://example.com:8080"))
        }
    }

    @Nested
    inner class PathNormalization {
        @Test
        fun `normalizes dot segments`() {
            assertEquals("https://example.com/b", normalizer.normalize("https://example.com/a/../b"))
        }

        @Test
        fun `removes trailing slash from path`() {
            assertEquals("https://example.com/path", normalizer.normalize("https://example.com/path/"))
        }

        @Test
        fun `preserves root path`() {
            assertEquals("https://example.com/", normalizer.normalize("https://example.com/"))
        }
    }

    @Nested
    inner class QueryParamNormalization {
        @Test
        fun `removes utm parameters`() {
            assertEquals(
                "https://example.com/page",
                normalizer.normalize("https://example.com/page?utm_source=google&utm_medium=cpc"),
            )
        }

        @Test
        fun `removes tracking parameters`() {
            assertEquals(
                "https://example.com/page",
                normalizer.normalize("https://example.com/page?fbclid=abc123"),
            )
        }

        @Test
        fun `sorts query params alphabetically`() {
            assertEquals(
                "https://example.com/page?a=1&b=2&c=3",
                normalizer.normalize("https://example.com/page?c=3&a=1&b=2"),
            )
        }

        @Test
        fun `preserves non-tracking params`() {
            assertEquals(
                "https://example.com/page?id=123",
                normalizer.normalize("https://example.com/page?id=123&utm_campaign=test"),
            )
        }
    }

    @Nested
    inner class TikTokNormalization {
        @Test
        fun `removes all query params from tiktok`() {
            assertEquals(
                "https://www.tiktok.com/@user/video/123",
                normalizer.normalize("https://www.tiktok.com/@user/video/123?is_copy_url=1&is_from_webapp=v1"),
            )
        }

        @Test
        fun `handles tiktok subdomain`() {
            assertEquals(
                "https://vm.tiktok.com/abc123",
                normalizer.normalize("https://vm.tiktok.com/abc123?foo=bar"),
            )
        }
    }

    @Nested
    inner class InstagramNormalization {
        @Test
        fun `removes all query params from instagram`() {
            assertEquals(
                "https://www.instagram.com/p/abc123",
                normalizer.normalize("https://www.instagram.com/p/abc123?igshid=xyz"),
            )
        }
    }

    @Nested
    inner class YoutubeNormalization {
        @Test
        fun `preserves v parameter for youtube watch`() {
            assertEquals(
                "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
                normalizer.normalize("https://www.youtube.com/watch?v=dQw4w9WgXcQ"),
            )
        }

        @Test
        fun `puts v parameter first for youtube`() {
            assertEquals(
                "https://www.youtube.com/watch?v=abc&list=xyz",
                normalizer.normalize("https://www.youtube.com/watch?list=xyz&v=abc"),
            )
        }

        @Test
        fun `removes utm from youtube`() {
            assertEquals(
                "https://www.youtube.com/watch?v=abc",
                normalizer.normalize("https://www.youtube.com/watch?v=abc&utm_source=share"),
            )
        }

        @Test
        fun `handles youtu be short urls and preserves non-tracking params`() {
            // si param is not in DROP_EXACT list, so it's preserved
            assertEquals(
                "https://youtu.be/abc123?si=tracking",
                normalizer.normalize("https://youtu.be/abc123?si=tracking"),
            )
        }

        @Test
        fun `removes utm params from youtu be`() {
            assertEquals(
                "https://youtu.be/abc123",
                normalizer.normalize("https://youtu.be/abc123?utm_source=share"),
            )
        }
    }

    @Nested
    inner class EdgeCases {
        @Test
        fun `trims whitespace`() {
            assertEquals("https://example.com/", normalizer.normalize("  https://example.com  "))
        }

        @Test
        fun `returns original on invalid url`() {
            val invalid = "not-a-url"
            assertEquals(invalid, normalizer.normalize(invalid))
        }

        @Test
        fun `returns original on unsupported scheme`() {
            val ftpUrl = "ftp://example.com"
            assertEquals(ftpUrl, normalizer.normalize(ftpUrl))
        }
    }
}

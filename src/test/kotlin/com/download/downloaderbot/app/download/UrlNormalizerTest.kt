package com.download.downloaderbot.app.download

import com.download.downloaderbot.util.case
import io.kotest.core.spec.style.FunSpec
import io.kotest.core.spec.style.scopes.FunSpecContainerScope
import io.kotest.matchers.shouldBe

class UrlNormalizerTest : FunSpec({

    val normalizer = UrlNormalizer()

    suspend fun FunSpecContainerScope.case(
        name: String,
        input: String,
        expected: String,
    ) = case(name, input, expected, normalizer::normalize)

    context("fallback behaviour") {
        case(
            name = "trims url and returns original when scheme is unsupported",
            input = "  ftp://example.com/resource  ",
            expected = "ftp://example.com/resource",
        )

        case(
            name = "returns trimmed original when scheme is missing",
            input = " example.com/path ",
            expected = "example.com/path",
        )

        case(
            name = "returns trimmed original when host is missing",
            input = " https:///path ",
            expected = "https:///path",
        )
    }

    context("scheme and host normalization") {
        case(
            name = "lowercases scheme and host",
            input = "HTTPS://Example.COM/SomePath",
            expected = "https://example.com/SomePath",
        )
    }

    context("port normalization") {
        case(
            name = "removes default http port 80",
            input = "http://example.com:80/path",
            expected = "http://example.com/path",
        )

        case(
            name = "removes default https port 443",
            input = "https://example.com:443/path",
            expected = "https://example.com/path",
        )

        case(
            name = "keeps non-default ports",
            input = "https://example.com:8443/path",
            expected = "https://example.com:8443/path",
        )
    }

    context("path normalization") {
        case(
            name = "adds leading slash when path is absent",
            input = "https://example.com",
            expected = "https://example.com/",
        )

        case(
            name = "removes trailing slash for non-root path",
            input = "https://example.com/some/path/",
            expected = "https://example.com/some/path",
        )

        case(
            name = "normalizes dot segments in path",
            input = "https://example.com/a/b/../c",
            expected = "https://example.com/a/c",
        )
    }

    context("generic query normalization") {
        case(
            name = "sorts query parameters by name and value",
            input = "https://example.com/path?b=2&a=3&a=1&c",
            expected = "https://example.com/path?a=1&a=3&b=2&c",
        )

        case(
            name = "drops tracking parameters",
            input = "https://example.com/path?utm_source=google&fbclid=123&gclid=321&msclkid=1&dclid=2&igshid=3&keep=1",
            expected = "https://example.com/path?keep=1",
        )
    }

    context("platforms: tiktok & instagram") {
        case(
            name = "drops all query parameters for TikTok",
            input = "https://www.tiktok.com/@user/video/123?lang=en&utm_source=foo",
            expected = "https://www.tiktok.com/@user/video/123",
        )

        case(
            name = "drops all query parameters for Instagram",
            input = "https://instagram.com/p/abc/?utm_source=foo&igshid=bar",
            expected = "https://instagram.com/p/abc",
        )
    }

    context("platforms: youtube watch") {
        case(
            name = "keeps v param first and removes tracking params",
            input = "https://www.youtube.com/watch?utm_source=google&v=videoId&fbclid=123&hl=en",
            expected = "https://www.youtube.com/watch?v=videoId&hl=en",
        )

        case(
            name = "keeps only first v parameter",
            input = "https://www.youtube.com/watch?v=first&v=second&hl=en",
            expected = "https://www.youtube.com/watch?v=first&hl=en",
        )

        case(
            name = "sorts other parameters after v",
            input = "https://www.youtube.com/watch?z=1&b=2&v=videoId&a=3",
            expected = "https://www.youtube.com/watch?v=videoId&a=3&b=2&z=1",
        )
    }

    context("platforms: youtube generic") {
        case(
            name = "non-watch youtube urls are treated as generic",
            input = "https://www.youtube.com/embed/videoId?b=2&a=1",
            expected = "https://www.youtube.com/embed/videoId?a=1&b=2",
        )

        case(
            name = "youtu.be urls are treated as youtube but without /watch special case",
            input = "https://youtu.be/videoId?b=2&a=1",
            expected = "https://youtu.be/videoId?a=1&b=2",
        )
    }

    test("normalization is idempotent for a typical http url") {
        val input = "https://Example.com/some/../path/?b=2&a=1&utm_source=google"

        val once = normalizer.normalize(input)
        val twice = normalizer.normalize(once)

        twice shouldBe once
    }
})

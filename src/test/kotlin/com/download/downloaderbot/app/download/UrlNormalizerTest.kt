package com.download.downloaderbot.app.download

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class UrlNormalizerTest : FunSpec({

    val normalizer = UrlNormalizer()

    context("fallback behaviour") {

        test("trims url and returns original when scheme is unsupported") {
            val input = "  ftp://example.com/resource  "

            val result = normalizer.normalize(input)

            result shouldBe "ftp://example.com/resource"
        }

        test("returns trimmed original when scheme is missing") {
            val input = " example.com/path "

            val result = normalizer.normalize(input)

            result shouldBe "example.com/path"
        }

        test("returns trimmed original when host is missing") {
            val input = " https:///path "

            val result = normalizer.normalize(input)

            result shouldBe "https:///path"
        }
    }

    context("scheme and host normalization") {

        test("lowercases scheme and host") {
            val input = "HTTPS://Example.COM/SomePath"

            val result = normalizer.normalize(input)

            result shouldBe "https://example.com/SomePath"
        }
    }

    context("port normalization") {

        test("removes default http port 80") {
            val input = "http://example.com:80/path"

            val result = normalizer.normalize(input)

            result shouldBe "http://example.com/path"
        }

        test("removes default https port 443") {
            val input = "https://example.com:443/path"

            val result = normalizer.normalize(input)

            result shouldBe "https://example.com/path"
        }

        test("keeps non-default ports") {
            val input = "https://example.com:8443/path"

            val result = normalizer.normalize(input)

            result shouldBe "https://example.com:8443/path"
        }
    }

    context("path normalization") {

        test("adds leading slash when path is absent") {
            val input = "https://example.com"

            val result = normalizer.normalize(input)

            result shouldBe "https://example.com/"
        }

        test("removes trailing slash for non-root path") {
            val input = "https://example.com/some/path/"

            val result = normalizer.normalize(input)

            result shouldBe "https://example.com/some/path"
        }

        test("normalizes dot segments in path") {
            val input = "https://example.com/a/b/../c"

            val result = normalizer.normalize(input)

            result shouldBe "https://example.com/a/c"
        }
    }

    context("generic query normalization") {

        test("sorts query parameters by name and value") {
            val input = "https://example.com/path?b=2&a=3&a=1&c"

            val result = normalizer.normalize(input)

            result shouldBe "https://example.com/path?a=1&a=3&b=2&c"
        }

        test("drops tracking parameters") {
            val input =
                "https://example.com/path?" +
                        "utm_source=google&fbclid=123&gclid=321&msclkid=1&dclid=2&igshid=3&keep=1"

            val result = normalizer.normalize(input)

            result shouldBe "https://example.com/path?keep=1"
        }
    }

    context("platform-specific behaviour") {

        test("drops all query parameters for TikTok") {
            val input = "https://www.tiktok.com/@user/video/123?lang=en&utm_source=foo"

            val result = normalizer.normalize(input)

            result shouldBe "https://www.tiktok.com/@user/video/123"
        }

        test("drops all query parameters for Instagram") {
            val input = "https://instagram.com/p/abc/?utm_source=foo&igshid=bar"

            val result = normalizer.normalize(input)

            result shouldBe "https://instagram.com/p/abc"
        }

        context("youtube watch urls") {

            test("keeps v param first and removes tracking params") {
                val input =
                    "https://www.youtube.com/watch?" +
                            "utm_source=google&v=videoId&fbclid=123&hl=en"

                val result = normalizer.normalize(input)

                result shouldBe "https://www.youtube.com/watch?v=videoId&hl=en"
            }

            test("keeps only first v parameter") {
                val input =
                    "https://www.youtube.com/watch?v=first&v=second&hl=en"

                val result = normalizer.normalize(input)

                result shouldBe "https://www.youtube.com/watch?v=first&hl=en"
            }

            test("sorts other parameters after v") {
                val input =
                    "https://www.youtube.com/watch?z=1&b=2&v=videoId&a=3"

                val result = normalizer.normalize(input)

                result shouldBe "https://www.youtube.com/watch?v=videoId&a=3&b=2&z=1"
            }
        }

        test("non-watch youtube urls are treated as generic") {
            val input = "https://www.youtube.com/embed/videoId?b=2&a=1"

            val result = normalizer.normalize(input)

            result shouldBe "https://www.youtube.com/embed/videoId?a=1&b=2"
        }

        test("youtu.be urls are treated as youtube but without /watch special case") {
            val input = "https://youtu.be/videoId?b=2&a=1"

            val result = normalizer.normalize(input)

            result shouldBe "https://youtu.be/videoId?a=1&b=2"
        }
    }

    test("normalization is idempotent for a typical http url") {
        val input =
            "https://Example.com/some/../path/?b=2&a=1&utm_source=google"

        val once = normalizer.normalize(input)
        val twice = normalizer.normalize(once)

        twice shouldBe once
    }
})

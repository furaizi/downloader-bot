package com.download.downloaderbot.bot.commands.util

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

private data class UrlCase(
    val name: String,
    val input: String,
    val expected: Boolean,
)

class UrlValidatorTest : FunSpec({

    urlCases(
        contextName = "invalid http urls",
        cases =
            listOf(
                UrlCase("blank string", "", false),
                UrlCase("spaces only", "   ", false),
                UrlCase("not a uri", "not a uri with spaces", false),
                UrlCase("http with space in host", "http://exa mple.com", false),
                UrlCase("ftp scheme", "ftp://example.com", false),
                UrlCase("mailto scheme", "mailto:user@example.com", false),
                UrlCase("file scheme", "file:///tmp/file.txt", false),
                UrlCase("missing host http://", "http://", false),
                UrlCase("missing host http:/", "http:/", false),
                UrlCase("only path with http scheme", "http:///only/path", false),
                UrlCase("only path with https scheme", "https:///only/path", false),
                UrlCase("relative path", "/relative/path", false),
                UrlCase("relative without slash", "relative/path", false),
                UrlCase("domain without scheme", "example.com", false),
                UrlCase("scheme-less protocol-relative", "//example.com", false),
            ),
    )

    urlCases(
        contextName = "valid http urls",
        cases =
            listOf(
                UrlCase("simple http", "http://example.com", true),
                UrlCase("simple https", "https://example.com", true),
                UrlCase("with path", "https://example.com/path", true),
                UrlCase("with query", "https://example.com/path?query=1", true),
                UrlCase("with port", "https://example.com:8443/path", true),
                UrlCase("localhost with port", "http://localhost:8080/api/v1/resource", true),
                UrlCase("ip address", "https://127.0.0.1", true),
            ),
    )
})

private fun FunSpec.urlCases(
    contextName: String,
    cases: List<UrlCase>,
) {
    val validator = UrlValidator()

    context(contextName) {
        cases.forEach { (name, input, expected) ->
            test(name) {
                validator.isHttpUrl(input) shouldBe expected
            }
        }
    }
}

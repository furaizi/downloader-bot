package com.download.downloaderbot.bot.commands.util

import com.download.downloaderbot.util.case
import io.kotest.core.spec.style.FunSpec
import io.kotest.core.spec.style.scopes.FunSpecContainerScope

class UrlValidatorTest : FunSpec({

    val urlValidator = UrlValidator()

    suspend fun FunSpecContainerScope.case(
        name: String,
        input: String,
        expected: Boolean,
    ) = case(name, input, expected, urlValidator::isHttpUrl)

    context("invalid http urls") {
        case("blank string", "", false)
        case("spaces only", "   ", false)
        case("not a uri", "not a uri with spaces", false)
        case("http with space in host", "http://exa mple.com", false)
        case("ftp scheme", "ftp://example.com", false)
        case("mailto scheme", "mailto:user@example.com", false)
        case("file scheme", "file:///tmp/file.txt", false)
        case("missing host http://", "http://", false)
        case("missing host http:/", "http:/", false)
        case("only path with http scheme", "http:///only/path", false)
        case("only path with https scheme", "https:///only/path", false)
        case("relative path", "/relative/path", false)
        case("relative without slash", "relative/path", false)
        case("domain without scheme", "example.com", false)
        case("scheme-less protocol-relative", "//example.com", false)
    }

    context("valid http urls") {
        case("simple http", "http://example.com", true)
        case("simple https", "https://example.com", true)
        case("with path", "https://example.com/path", true)
        case("with query", "https://example.com/path?query=1", true)
        case("with port", "https://example.com:8443/path", true)
        case("localhost with port", "http://localhost:8080/api/v1/resource", true)
        case("ip address", "https://127.0.0.1", true)
    }
})

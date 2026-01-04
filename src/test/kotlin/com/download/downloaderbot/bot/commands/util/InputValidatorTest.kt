package com.download.downloaderbot.bot.commands.util

import com.download.downloaderbot.util.case
import io.kotest.core.spec.style.FunSpec
import io.kotest.core.spec.style.scopes.FunSpecContainerScope

class InputValidatorTest : FunSpec({

    val inputValidator = InputValidator()

    suspend fun FunSpecContainerScope.caseHttpUrl(
        name: String,
        input: String,
        expected: Boolean,
    ) = case(name, input, expected, inputValidator::isHttpUrl)

    suspend fun FunSpecContainerScope.caseInstagramUsername(
        name: String,
        input: String,
        expected: Boolean,
    ) = case(name, input, expected, inputValidator::isInstagramUsername)

    context("invalid http urls") {
        caseHttpUrl("blank string", "", false)
        caseHttpUrl("spaces only", "   ", false)
        caseHttpUrl("not a uri", "not a uri with spaces", false)
        caseHttpUrl("http with space in host", "http://exa mple.com", false)
        caseHttpUrl("ftp scheme", "ftp://example.com", false)
        caseHttpUrl("mailto scheme", "mailto:user@example.com", false)
        caseHttpUrl("file scheme", "file:///tmp/file.txt", false)
        caseHttpUrl("missing host http://", "http://", false)
        caseHttpUrl("missing host http:/", "http:/", false)
        caseHttpUrl("only path with http scheme", "http:///only/path", false)
        caseHttpUrl("only path with https scheme", "https:///only/path", false)
        caseHttpUrl("relative path", "/relative/path", false)
        caseHttpUrl("relative without slash", "relative/path", false)
        caseHttpUrl("domain without scheme", "example.com", false)
        caseHttpUrl("scheme-less protocol-relative", "//example.com", false)
    }

    context("valid http urls") {
        caseHttpUrl("simple http", "http://example.com", true)
        caseHttpUrl("simple https", "https://example.com", true)
        caseHttpUrl("with path", "https://example.com/path", true)
        caseHttpUrl("with query", "https://example.com/path?query=1", true)
        caseHttpUrl("with port", "https://example.com:8443/path", true)
        caseHttpUrl("localhost with port", "http://localhost:8080/api/v1/resource", true)
        caseHttpUrl("ip address", "https://127.0.0.1", true)
    }

    context("invalid instagram usernames") {
        caseInstagramUsername("blank string", "", false)
        caseInstagramUsername("spaces only", "   ", false)
        caseInstagramUsername("contains hyphen", "user-name", false)
        caseInstagramUsername("contains at sign", "@username", false)
        caseInstagramUsername("contains spaces inside", "user name", false)
        caseInstagramUsername("contains slash", "user/name", false)
        caseInstagramUsername("contains emoji", "user😀", false)
        caseInstagramUsername("contains cyrillic", "юзернейм", false)
        caseInstagramUsername("leading dot", ".username", false)
        caseInstagramUsername("trailing dot", "username.", false)
        caseInstagramUsername("consecutive dots", "user..name", false)
        caseInstagramUsername("too long 31 chars", "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", false)
    }

    context("valid instagram usernames") {
        caseInstagramUsername("single letter", "a", true)
        caseInstagramUsername("single digit", "7", true)
        caseInstagramUsername("simple", "username", true)
        caseInstagramUsername("with dot in middle", "user.name", true)
        caseInstagramUsername("multiple dots (non-consecutive)", "u.s.e.r.n.a.m.e", true)
        caseInstagramUsername("mixed case", "UserName", true)
        caseInstagramUsername("leading/trailing spaces trimmed", "  username  ", true)
        caseInstagramUsername("max length 30 chars", "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", true)
    }
})

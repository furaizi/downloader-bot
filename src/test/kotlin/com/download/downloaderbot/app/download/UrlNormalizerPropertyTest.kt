package com.download.downloaderbot.app.download

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.Codepoint
import io.kotest.property.arbitrary.alphanumeric
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.filter
import io.kotest.property.arbitrary.flatMap
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.set
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import java.net.URI

class UrlNormalizerPropertyTest : StringSpec() {

    private val normalizer = UrlNormalizer()

    private val tokenArb = Arb.string(3..10, Codepoint.alphanumeric())

    private val tldArb = Arb.string(2..10, Codepoint.alphanumeric())
        .filter { it.first().isLetter() }

    private val hostArb = Arb.list(tokenArb, 0..2)
        .flatMap { subdomains ->
            tldArb.map { tld ->
                (subdomains + tld).joinToString(".")
            }
        }

    private val pathArb = Arb.list(tokenArb, 0..4)
        .map { "/" + it.joinToString("/") }

    private val tikTokInstaHostArb = Arb.element(
        "tiktok.com",
        "www.tiktok.com",
        "m.tiktok.com",
        "instagram.com",
        "www.instagram.com"
    )

    private val paramArb = tokenArb.map { name -> "$name=${name}val" }

    private val queryArb = Arb.list(paramArb, 1..4).map { params ->
        params.joinToString("&")
    }

    private val youtubeHostArb = Arb.element(
        "youtube.com",
        "www.youtube.com",
        "m.youtube.com"
    )

    private val otherNameArb = Arb
        .string(1..5, Codepoint.alphanumeric())
        .filter { !it.startsWith("utm_", ignoreCase = true) }

    init {

        "normalize is idempotent for any string" {
            checkAll<String> { raw ->
                val once = normalizer.normalize(raw)
                val twice = normalizer.normalize(once)

                twice shouldBe once
            }
        }

        "http and https default ports are removed" {
            checkAll(hostArb, pathArb) { host, path ->
                val httpWithPort = "http://$host:80$path"
                val httpNoPort = "http://$host$path"

                val httpsWithPort = "https://$host:443$path"
                val httpsNoPort = "https://$host$path"

                normalizer.normalize(httpWithPort) shouldBe normalizer.normalize(httpNoPort)
                normalizer.normalize(httpsWithPort) shouldBe normalizer.normalize(httpsNoPort)
            }
        }

        "tracking params do not affect canonical url" {
            checkAll(hostArb, pathArb, tokenArb, tokenArb, tokenArb, tokenArb) {
                    host, path, v1, v2, t1, t2 ->
                val base = "https://$host$path"

                val cleanQuery = "a=$v1&b=$v2"
                val url1 = "$base?$cleanQuery&utm_source=$t1&fbclid=$t2"
                val url2 = "$base?$cleanQuery&gclid=$t1&msclkid=$t2"

                normalizer.normalize(url1) shouldBe normalizer.normalize(url2)
            }
        }

        "tiktok and instagram queries are always dropped" {
            checkAll(tikTokInstaHostArb, pathArb, queryArb) { host, path, query ->
                val url = "https://$host$path?$query"

                val normalized = normalizer.normalize(url)
                val uri = URI(normalized)

                uri.query shouldBe null
            }
        }

        "youtube watch keeps v first and sorts remaining params" {
            val valueArb = tokenArb

            checkAll(
                youtubeHostArb,
                valueArb,
                Arb.set(otherNameArb, 0..4),
                Arb.list(valueArb, 0..4)
            ) { host, videoId, otherNamesSet, otherValues ->

                val otherNames = otherNamesSet.toList()

                val params = buildList {
                    add("v=$videoId")
                    otherNames.forEachIndexed { index, name ->
                        val value = otherValues.getOrNull(index) ?: "x"
                        add("$name=$value")
                    }
                }.shuffled()

                val url = "https://$host/watch?${params.joinToString("&")}"

                val normalizedUri = URI(normalizer.normalize(url))
                val pairs = normalizedUri.queryPairs()

                if (pairs.isNotEmpty()) {
                    pairs.first().first shouldBe "v"

                    if (pairs.size > 1) {
                        val tail = pairs.drop(1)

                        val sortedTail = tail.sortedWith(
                            compareBy<Pair<String, String>> { it.first.lowercase() }
                                .thenBy { it.second }
                        )

                        tail shouldBe sortedTail
                    }
                }
            }
        }
    }

    private fun URI.queryPairs(): List<Pair<String, String>> =
        rawQuery
            ?.split('&')
            ?.filter { it.isNotBlank() }
            ?.map { part ->
                val name = part.substringBefore("=")
                val value = part.substringAfter("=", "")
                name to value
            }
            .orEmpty()
}

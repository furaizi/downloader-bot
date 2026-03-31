package com.download.downloaderbot.app.download

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.springframework.stereotype.Component

private val DROP_EXACT = setOf("fbclid", "gclid", "msclkid", "dclid", "igshid")
private val ALPHABETICAL_PARAM_COMPARATOR = compareBy<Pair<String, String?>>({ it.first.lowercase() }, { it.second.orEmpty() })

@Component
class UrlNormalizer {

    fun normalize(url: String): String {
        val trimmed = url.trim()
        val httpUrl = trimmed.toHttpUrlOrNull() ?: return trimmed

        return httpUrl.newBuilder()
            .applyPlatformRules(httpUrl)
            .build()
            .toString()
            .removeRedundantTrailingSlash(httpUrl)
    }

    private fun HttpUrl.Builder.applyPlatformRules(url: HttpUrl): HttpUrl.Builder = apply {
        val host = url.host
        when {
            host.matchesDomain("tiktok.com") || host.matchesDomain("instagram.com") -> {
                query(null)
            }
            host.matchesDomain("youtube.com") || host.matchesDomain("youtu.be") -> {
                val prioritize = if (url.encodedPath == "/watch") "v" else null
                applyNormalizedQuery(url, prioritizeKey = prioritize)
            }
            else -> {
                applyNormalizedQuery(url)
            }
        }
    }

    private fun HttpUrl.Builder.applyNormalizedQuery(url: HttpUrl, prioritizeKey: String? = null) {
        val validParams = url.queryParameterNames
            .filterNot { it.isNoise() }
            .flatMap { name -> url.queryParameterValues(name).map { name to it } }

        query(null)
        if (validParams.isEmpty())
            return

        val (priority, others) = if (prioritizeKey != null) {
            validParams.partition { it.first == prioritizeKey }
        } else {
            emptyList<Pair<String, String?>>() to validParams
        }

        val finalParams = priority + others.sortedWith(ALPHABETICAL_PARAM_COMPARATOR)

        finalParams.forEach { (name, value) ->
            addQueryParameter(name, value)
        }
    }


    private fun String.removeRedundantTrailingSlash(url: HttpUrl): String =
        if (this.endsWith("/") && url.encodedPath.length > 1)
            this.removeSuffix("/")
        else
            this

    private fun String.matchesDomain(base: String): Boolean =
        this == base || this.endsWith(".$base")

    private fun String.isNoise(): Boolean =
        this.startsWith("utm_", ignoreCase = true) || this.lowercase() in DROP_EXACT
}

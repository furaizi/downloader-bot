package com.download.downloaderbot.app.download.media

import org.springframework.stereotype.Component
import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

private const val UNSPECIFIED_PORT = -1
private const val HTTP_DEFAULT_PORT = 80
private const val HTTPS_DEFAULT_PORT = 443
private val DROP_EXACT = setOf("fbclid", "gclid", "msclkid", "dclid", "igshid")

@Component
class UrlNormalizer {
    private fun isNoise(name: String) = name.startsWith("utm_", ignoreCase = true) || name.lowercase() in DROP_EXACT

    private data class Platforms(
        val isTiktok: Boolean,
        val isInstagram: Boolean,
        val isYoutube: Boolean,
    ) {
        val dropAllQuery: Boolean get() = isTiktok || isInstagram
    }

    fun normalize(url: String): String {
        val trimmed = url.trim()
        return runCatching { normalizedUrl(trimmed) }
            .getOrDefault(trimmed)
    }

    private fun normalizedUrl(trimmed: String): String {
        val uri = URI(trimmed)
        val scheme = requireNotNull(uri.scheme?.lowercase()) { "Missing scheme" }
        require(scheme == "http" || scheme == "https") { "Unsupported scheme" }

        val host = requireNotNull(uri.host?.lowercase()) { "Missing host" }
        val platforms = detectPlatforms(host)

        val port = normalizePort(scheme, uri.port)
        val path = normalizePath(uri.rawPath)
        val query = buildNormalizedQuery(uri.rawQuery, path, platforms)

        return URI(scheme, uri.userInfo, host, port, path, query, null).toASCIIString()
    }

    private fun detectPlatforms(host: String): Platforms {
        fun matches(base: String) = host == base || host.endsWith(".$base")

        return Platforms(
            isTiktok = matches("tiktok.com"),
            isInstagram = matches("instagram.com"),
            isYoutube = matches("youtube.com") || matches("youtu.be"),
        )
    }

    private fun normalizePort(
        scheme: String,
        port: Int,
    ): Int =
        when {
            port == UNSPECIFIED_PORT -> UNSPECIFIED_PORT
            scheme == "http" && port == HTTP_DEFAULT_PORT -> UNSPECIFIED_PORT
            scheme == "https" && port == HTTPS_DEFAULT_PORT -> UNSPECIFIED_PORT
            else -> port
        }

    private fun normalizePath(rawPath: String?): String =
        URI(null, null, (rawPath ?: "/").ifEmpty { "/" }, null)
            .normalize().path
            .let { if (it.length > 1 && it.endsWith('/')) it.dropLast(1) else it }

    private fun buildNormalizedQuery(
        rawQuery: String?,
        path: String,
        platforms: Platforms,
    ): String? {
        if (platforms.dropAllQuery) return null

        val params =
            parseQuery(rawQuery)
                .filterNot { isNoise(it.first) }

        val normalizedParams =
            if (platforms.isYoutube && path == "/watch") {
                normalizeYoutubeParams(params)
            } else {
                params.sortedWith(compareBy({ it.first.lowercase() }, { it.second ?: "" }))
            }

        return buildQuery(normalizedParams)
    }

    private fun normalizeYoutubeParams(params: List<Pair<String, String?>>): List<Pair<String, String?>> {
        if (params.isEmpty()) return params

        val cleaned = params.filterNot { isNoise(it.first) }
        val (vParams, others) = cleaned.partition { it.first.equals("v", ignoreCase = true) }
        val v = vParams.firstOrNull()

        val sortedOthers = others.sortedWith(compareBy({ it.first.lowercase() }, { it.second ?: "" }))

        return listOfNotNull(v) + sortedOthers
    }

    private fun parseQuery(q: String?): List<Pair<String, String?>> {
        if (q.isNullOrBlank()) return emptyList()
        val utf8 = StandardCharsets.UTF_8
        return q.split('&')
            .filter { it.isNotBlank() }
            .map { pair ->
                val i = pair.indexOf('=')
                if (i < 0) {
                    URLDecoder.decode(pair, utf8) to null
                } else {
                    URLDecoder.decode(pair.take(i), utf8) to
                        URLDecoder.decode(pair.substring(i + 1), utf8)
                }
            }
    }

    private fun buildQuery(params: List<Pair<String, String?>>): String? {
        if (params.isEmpty()) return null
        val utf8 = StandardCharsets.UTF_8
        return params.joinToString("&") { (n, v) ->
            val en = URLEncoder.encode(n, utf8)
            if (v == null) en else "$en=${URLEncoder.encode(v, utf8)}"
        }
    }
}

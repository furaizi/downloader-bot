package com.download.downloaderbot.app.download

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

    private fun hostMatches(
        host: String?,
        base: String,
    ) = host != null && (host == base || host.endsWith(".$base"))

    fun normalize(url: String): String {
        val s = url.trim()
        val uri = runCatching { URI(s) }.getOrElse { return s }

        val scheme = uri.scheme?.lowercase() ?: return s
        if (scheme != "http" && scheme != "https") return s

        val host = uri.host?.lowercase() ?: return s
        val isTiktok = hostMatches(host, "tiktok.com")
        val isInstagram = hostMatches(host, "instagram.com")
        val isYoutube = hostMatches(host, "youtube.com") || hostMatches(host, "youtu.be")

        val dropAllQuery = isTiktok || isInstagram

        val port =
            when {
                uri.port == UNSPECIFIED_PORT -> UNSPECIFIED_PORT
                scheme == "http" && uri.port == HTTP_DEFAULT_PORT -> UNSPECIFIED_PORT
                scheme == "https" && uri.port == HTTPS_DEFAULT_PORT -> UNSPECIFIED_PORT
                else -> uri.port
            }

        val rawPath = uri.rawPath ?: "/"
        val path =
            URI(null, null, if (rawPath.isEmpty()) "/" else rawPath, null)
                .normalize().path
                .let { if (it.length > 1 && it.endsWith('/')) it.dropLast(1) else it }

        val query =
            when {
                dropAllQuery -> null
                else -> {
                    val params =
                        parseQuery(uri.rawQuery)
                            .filterNot { isNoise(it.first) }

                    val normalizedParams =
                        if (isYoutube && path == "/watch") {
                            normalizeYoutubeParams(params)
                        } else {
                            params.sortedWith(compareBy({ it.first.lowercase() }, { it.second ?: "" }))
                        }

                    buildQuery(normalizedParams)
                }
            }

        return runCatching {
            URI(scheme, uri.userInfo, host, port, path, query, null).toASCIIString()
        }.getOrElse { s }
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

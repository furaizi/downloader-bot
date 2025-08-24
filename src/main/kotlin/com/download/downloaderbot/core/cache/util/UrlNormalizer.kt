package com.download.downloaderbot.core.cache.util

import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object UrlNormalizer {
    private val DROP_EXACT = setOf("fbclid", "gclid", "msclkid", "dclid", "igshid")
    private fun isNoise(name: String) =
        name.startsWith("utm_", ignoreCase = true) || name.lowercase() in DROP_EXACT

    private fun hostMatches(host: String?, base: String) =
        host != null && (host == base || host.endsWith(".$base"))

    fun normalize(url: String): String {
        val s = url.trim()
        val uri = runCatching { URI(s) }.getOrElse { return s }

        val scheme = uri.scheme?.lowercase() ?: return s
        if (scheme != "http" && scheme != "https") return s

        val host = uri.host?.lowercase() ?: return s
        val dropAllQuery = hostMatches(host, "tiktok.com") || hostMatches(host, "instagram.com")

        val port = when {
            uri.port == -1 -> -1
            scheme == "http" && uri.port == 80 -> -1
            scheme == "https" && uri.port == 443 -> -1
            else -> uri.port
        }

        val rawPath = uri.rawPath ?: "/"
        val path = URI(null, null, if (rawPath.isEmpty()) "/" else rawPath, null)
            .normalize().path
            .let { if (it.length > 1 && it.endsWith('/')) it.dropLast(1) else it }

        val query = when {
            dropAllQuery -> null
            else -> buildQuery(
                parseQuery(uri.rawQuery)
                    .filterNot { isNoise(it.first) }
                    .sortedWith(compareBy({ it.first.lowercase() }, { it.second ?: "" }))
            )
        }

        return runCatching {
            URI(scheme, uri.userInfo, host, port, path, query, null).toASCIIString()
        }.getOrElse { s }
    }

    private fun parseQuery(q: String?): List<Pair<String, String?>> {
        if (q.isNullOrBlank()) return emptyList()
        val utf8 = StandardCharsets.UTF_8
        return q.split('&').filter { it.isNotBlank() }.map { pair ->
            val i = pair.indexOf('=')
            if (i < 0) URLDecoder.decode(pair, utf8) to null
            else URLDecoder.decode(pair.substring(0, i), utf8) to URLDecoder.decode(pair.substring(i + 1), utf8)
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
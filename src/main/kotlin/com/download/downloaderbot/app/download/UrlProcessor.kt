package com.download.downloaderbot.app.download

import com.download.downloaderbot.core.net.FinalUrlResolver
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.springframework.stereotype.Component

@Component
class UrlProcessor(
    private val resolver: FinalUrlResolver,
    private val normalizer: UrlNormalizer,
) {
    suspend fun process(url: String): String {
        val normalized = normalizer.normalize(url)

        if (shouldSkipResolve(normalized)) {
            return normalized
        }

        val resolved = resolver.resolve(normalized)
        return normalizer.normalize(resolved)
    }

    private fun shouldSkipResolve(url: String): Boolean {
        val host = url.toHttpUrlOrNull()
            ?.host
            ?.lowercase()
            ?: return false

        return host == "instagram.com" || host.endsWith(".instagram.com")
    }
}

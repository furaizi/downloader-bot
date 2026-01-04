package com.download.downloaderbot.app.download

import com.download.downloaderbot.core.net.FinalUrlResolver
import org.springframework.stereotype.Component
import java.net.URI

@Component
class UrlOps(
    private val resolver: FinalUrlResolver,
    private val normalizer: UrlNormalizer
) {
    suspend fun finalOf(url: String): String {
        val normalized = normalizer.normalize(url)

        if (shouldSkipResolve(normalized)) {
            return normalized
        }

        val resolved = resolver.resolve(normalized)
        return normalizer.normalize(resolved)
    }

    private fun shouldSkipResolve(url: String): Boolean =
        runCatching {
            val uri = URI(url.trim())
            val host = uri.host?.lowercase().orEmpty()

            host == "instagram.com" || host.endsWith(".instagram.com")
        }.getOrDefault(false)
}

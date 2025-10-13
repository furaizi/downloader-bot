package com.download.downloaderbot.app.download

import com.download.downloaderbot.core.net.FinalUrlResolver
import org.springframework.stereotype.Component

@Component
class UrlOps(
    private val resolver: FinalUrlResolver,
    private val normalizer: UrlNormalizer,
) {
    suspend fun finalOf(url: String): String {
        val resolved = resolver.resolve(url)
        return normalizer.normalize(resolved)
    }
}
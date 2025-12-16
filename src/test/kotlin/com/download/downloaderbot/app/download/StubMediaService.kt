package com.download.downloaderbot.app.download

import com.download.downloaderbot.core.domain.Media
import java.util.concurrent.ConcurrentHashMap

class StubMediaService : MediaService {
    private val responses = ConcurrentHashMap<String, List<Media>>()

    fun stubDownload(
        url: String,
        result: List<Media>,
    ) {
        responses[url] = result
    }

    fun reset() = responses.clear()

    override suspend fun supports(url: String): Boolean = responses.containsKey(url)

    override suspend fun download(url: String): List<Media> = responses[url] ?: emptyList()
}

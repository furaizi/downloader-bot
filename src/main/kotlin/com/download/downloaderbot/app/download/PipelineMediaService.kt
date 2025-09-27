package com.download.downloaderbot.app.download

import com.download.downloaderbot.core.domain.Media

typealias Handler = suspend (String) -> List<Media>

fun interface MediaInterceptor {
    suspend fun invoke(url: String, next: Handler): List<Media>
}

class PipelineMediaService(
    private val core: MediaService,
    interceptors: List<MediaInterceptor>
) : MediaService {

    private val chain: Handler = interceptors
        .reversed()
        .fold({ u -> core.download(u) }) { next, i ->
            { u -> i.invoke(u, next) }
        }

    override suspend fun download(url: String): List<Media> = chain(url)
}

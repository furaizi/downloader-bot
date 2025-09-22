package com.download.downloaderbot.core.service.interceptor

import com.download.downloaderbot.core.domain.Media
import com.download.downloaderbot.core.service.MediaDownloadService

typealias Handler = suspend (String) -> List<Media>

fun interface MediaInterceptor {
    suspend fun invoke(url: String, next: Handler): List<Media>
}

class PipelineMediaDownloadService(
    private val core: MediaDownloadService,
    interceptors: List<MediaInterceptor>
) : MediaDownloadService {

    private val chain: Handler = interceptors
        .reversed()
        .fold({ u -> core.download(u) }) { next, i ->
            { u -> i.invoke(u, next) }
        }

    override suspend fun download(url: String): List<Media> = chain(url)
}
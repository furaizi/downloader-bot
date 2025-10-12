package com.download.downloaderbot.app.download

import com.download.downloaderbot.core.domain.Media
import com.download.downloaderbot.core.downloader.MediaProvider

typealias MediaHandler = suspend (String) -> List<Media>
typealias BoolHandler = suspend (String) -> Boolean

fun interface MediaInterceptor {
    suspend fun invoke(
        url: String,
        next: MediaHandler,
    ): List<Media>
}

fun interface SupportsInterceptor {
    suspend fun invoke(
        url: String,
        next: BoolHandler,
    ): Boolean
}

class PipelineMediaService(
    private val core: MediaProvider,
    supportsInterceptors: List<SupportsInterceptor>,
    mediaInterceptors: List<MediaInterceptor>,
) : MediaService {
    private val supportsChain = buildChain(
        supportsInterceptors,
        terminal = { u -> core.supports(u) },
        invoke = { i, u, next -> i.invoke(u, next) }
    )

    private val downloadChain = buildChain(
        mediaInterceptors,
        terminal = { u -> core.download(u) },
        invoke = { i, u, next -> i.invoke(u, next) }
    )

    override suspend fun supports(url: String): Boolean = supportsChain(url)
    override suspend fun download(url: String): List<Media> = downloadChain(url)

    private fun <R, I> buildChain(
        interceptors: List<I>,
        terminal: suspend (String) -> R,
        invoke: suspend (I, String, suspend (String) -> R) -> R
    ): suspend (String) -> R =
        interceptors
            .asReversed()
            .fold(terminal) { next, i -> { u -> invoke(i, u, next) } }
}

package com.download.downloaderbot.core.concurrency

interface DownloadSlots {
    suspend fun <T> withSlotOrThrow(
        url: String,
        block: suspend () -> T,
    ): T
}

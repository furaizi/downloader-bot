package com.download.downloaderbot.core.concurrency

import com.download.downloaderbot.core.config.properties.ConcurrencyProperties
import com.download.downloaderbot.core.downloader.BusyException
import kotlinx.coroutines.sync.Semaphore
import org.springframework.stereotype.Component

@Component
class DownloadSlots(
    val props: ConcurrencyProperties
) {
    private val sem = Semaphore(props.maxDownloads)

    suspend fun <T> withSlotOrThrow(url: String, block: suspend () -> T): T {
        val acquired = sem.tryAcquire()
        if (!acquired) throw BusyException(url)
        try {
            return block()
        } finally {
            sem.release()
        }
    }
}
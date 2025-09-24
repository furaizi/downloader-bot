package com.download.downloaderbot.core.concurrency

import com.download.downloaderbot.core.config.properties.ConcurrencyProperties
import com.download.downloaderbot.core.downloader.BusyException
import kotlinx.coroutines.sync.Semaphore
import org.springframework.stereotype.Component

@Component
class SemaphoreDownloadSlots(
    val props: ConcurrencyProperties
) : DownloadSlots {
    private val sem = Semaphore(props.maxDownloads)

    override suspend fun <T> withSlotOrThrow(url: String, block: suspend () -> T): T {
        val acquired = sem.tryAcquire()
        if (!acquired) throw BusyException(url)
        try {
            return block()
        } finally {
            sem.release()
        }
    }
}
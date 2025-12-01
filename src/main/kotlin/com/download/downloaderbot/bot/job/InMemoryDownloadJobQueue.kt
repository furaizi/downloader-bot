package com.download.downloaderbot.bot.job

import com.download.downloaderbot.app.config.properties.ConcurrencyProperties
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import org.springframework.stereotype.Component

@Component
class InMemoryDownloadJobQueue(
    private val props: ConcurrencyProperties,
    private val executor: DownloadJobExecutor,
    private val botScope: CoroutineScope,
) : DownloadJobQueue {

    private val channel = Channel<DownloadJob>(capacity = Channel.UNLIMITED)

    init {
        repeat(props.maxDownloads) {
            botScope.launch {
                for (job in channel) {
                    executor.execute(job)
                }
            }
        }
    }

    override suspend fun submit(job: DownloadJob) {
        channel.send(job)
    }
}
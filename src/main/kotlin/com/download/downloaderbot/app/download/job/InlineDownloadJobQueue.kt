package com.download.downloaderbot.app.download.job

import org.springframework.stereotype.Component

@Component
class InlineDownloadJobQueue(
    private val executor: DownloadJobExecutor,
) : DownloadJobQueue {

    override suspend fun submit(job: DownloadJob) {
        executor.execute(job)
    }
}
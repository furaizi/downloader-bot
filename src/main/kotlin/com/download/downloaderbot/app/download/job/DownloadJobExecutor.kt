package com.download.downloaderbot.app.download.job

interface DownloadJobExecutor {
    suspend fun execute(job: DownloadJob)
}
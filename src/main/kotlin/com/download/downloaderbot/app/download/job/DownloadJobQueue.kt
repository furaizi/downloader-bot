package com.download.downloaderbot.app.download.job

interface DownloadJobQueue {
    suspend fun submit(job: DownloadJob)
}
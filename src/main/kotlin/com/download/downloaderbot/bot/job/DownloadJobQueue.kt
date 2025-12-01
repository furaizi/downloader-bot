package com.download.downloaderbot.bot.job

interface DownloadJobQueue {
    suspend fun submit(job: DownloadJob)
}

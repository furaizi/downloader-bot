package com.download.downloaderbot.bot.job

interface DownloadJobExecutor {
    suspend fun execute(job: DownloadJob)
}
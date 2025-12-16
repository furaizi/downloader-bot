package com.download.downloaderbot.infra.process.runner

import com.download.downloaderbot.core.downloader.ToolExecutionException

class FailingProcessRunner(
    private val throwable: RuntimeException = ToolExecutionException("yt-dlp", 1, "boom"),
) : ProcessRunner {
    override suspend fun run(
        args: List<String>,
        url: String,
    ): String {
        throw throwable
    }
}

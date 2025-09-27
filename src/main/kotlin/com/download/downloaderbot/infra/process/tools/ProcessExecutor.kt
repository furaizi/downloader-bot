package com.download.downloaderbot.infra.process.tools

interface ProcessExecutor {
    suspend fun run(args: List<String>, url: String): String
}
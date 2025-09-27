package com.download.downloaderbot.infra.process.runner

interface ProcessRunner {
    suspend fun run(args: List<String>, url: String): String
}
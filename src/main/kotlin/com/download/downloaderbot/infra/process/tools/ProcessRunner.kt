package com.download.downloaderbot.infra.process.tools

interface ProcessRunner {
    suspend fun run(args: List<String>, url: String): String
}
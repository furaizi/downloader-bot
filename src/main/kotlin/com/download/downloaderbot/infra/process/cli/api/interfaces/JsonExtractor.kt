package com.download.downloaderbot.infra.process.cli.api.interfaces

interface JsonExtractor {
    suspend fun extract(source: String): String
}
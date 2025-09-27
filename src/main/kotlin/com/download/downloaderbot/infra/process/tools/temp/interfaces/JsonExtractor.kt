package com.download.downloaderbot.infra.process.tools.temp.interfaces

interface JsonExtractor {
    suspend fun extract(source: String): String
}
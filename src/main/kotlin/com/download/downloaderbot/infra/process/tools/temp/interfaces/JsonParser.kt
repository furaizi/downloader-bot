package com.download.downloaderbot.infra.process.tools.temp.interfaces

interface JsonParser<T> {
    suspend fun parse(json: String): T
}
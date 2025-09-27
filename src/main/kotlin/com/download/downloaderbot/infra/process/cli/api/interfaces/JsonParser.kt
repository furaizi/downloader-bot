package com.download.downloaderbot.infra.process.cli.api.interfaces

interface JsonParser<T> {
    suspend fun parse(json: String): T
}
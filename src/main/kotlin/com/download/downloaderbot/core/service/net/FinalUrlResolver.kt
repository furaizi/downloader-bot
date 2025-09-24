package com.download.downloaderbot.core.service.net

interface FinalUrlResolver {
    suspend fun resolve(url: String): String
}
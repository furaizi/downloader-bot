package com.download.downloaderbot.core.net

interface FinalUrlResolver {
    suspend fun resolve(url: String): String
}

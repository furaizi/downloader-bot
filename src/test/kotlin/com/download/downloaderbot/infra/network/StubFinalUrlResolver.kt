package com.download.downloaderbot.infra.network

import com.download.downloaderbot.core.net.FinalUrlResolver

class StubFinalUrlResolver : FinalUrlResolver {
    override suspend fun resolve(url: String) = url
}
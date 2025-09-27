package com.download.downloaderbot.infra.providers.interfaces

import com.download.downloaderbot.infra.providers.interfaces.DownloadPath

interface PathTemplateGenerator {
    fun generate(url: String): DownloadPath
}
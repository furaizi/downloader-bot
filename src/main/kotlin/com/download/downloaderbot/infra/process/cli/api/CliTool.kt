package com.download.downloaderbot.infra.process.cli.api

import com.download.downloaderbot.core.domain.Media

interface CliTool {
    val toolId: ToolId
    suspend fun download(url: String): List<Media>
}

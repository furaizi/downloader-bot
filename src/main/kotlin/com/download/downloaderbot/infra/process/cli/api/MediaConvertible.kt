package com.download.downloaderbot.infra.process.cli.api

import com.download.downloaderbot.core.domain.Media
import java.nio.file.Path

interface MediaConvertible {
    fun toMedia(
        filePath: Path,
        sourceUrl: String,
    ): Media
}

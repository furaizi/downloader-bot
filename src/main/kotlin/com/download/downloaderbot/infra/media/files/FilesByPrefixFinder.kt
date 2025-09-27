package com.download.downloaderbot.infra.media.files

import java.nio.file.Path

interface FilesByPrefixFinder {
    suspend fun find(prefix: String, dir: Path): List<Path>
}
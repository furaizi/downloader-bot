package com.download.downloaderbot.infra.providers.interfaces

import java.nio.file.Path

interface FilesByPrefixFinder {
    suspend fun find(prefix: String, dir: Path): List<Path>
}
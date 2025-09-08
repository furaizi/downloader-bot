package com.download.downloaderbot.core.downloader.tools.util.filefinder

import java.nio.file.Path

interface FilesByPrefixFinder {
    suspend fun find(prefix: String, dir: Path): List<Path>
}
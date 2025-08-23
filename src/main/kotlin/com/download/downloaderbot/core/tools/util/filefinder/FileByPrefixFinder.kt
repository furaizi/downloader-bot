package com.download.downloaderbot.core.tools.util.filefinder

import java.nio.file.Path

interface FileByPrefixFinder {

    suspend fun find(prefix: String, dir: Path): List<Path>
}
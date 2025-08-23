package com.download.downloaderbot.core.tools.util.filefinder

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.FileTime
import kotlin.io.path.isRegularFile
import kotlin.io.path.name
import kotlin.streams.asSequence
import kotlin.use

private val log = KotlinLogging.logger {}

@Component
class DefaultFileByPrefixFinder : FileByPrefixFinder {

    override suspend fun find(prefix: String, dir: Path): List<Path> {
        val files = findAllMatchingFile(prefix, dir)
        if (files.isEmpty())
            throw FileByPrefixNotFoundException(prefix, dir)
        log.info { "File found with prefix '$prefix' in directory '$dir': ${files.first()}" }
        return files
    }

    private suspend fun findAllMatchingFile(
        prefix: String,
        dir: Path
    ): List<Path> = withContext(Dispatchers.IO) {
        Files.list(dir).use { stream ->
            stream.asSequence()
                .filter { it.isRegularFile() && it.name.startsWith(prefix) }
                .sortedBy { getLastModifiedTime(it) }
                .toList()
        }
    }

    private fun getLastModifiedTime(path: Path): FileTime =
        runCatching { Files.getLastModifiedTime(path) }
            .getOrDefault(FileTime.fromMillis(0))
}
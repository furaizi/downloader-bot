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

    override suspend fun find(prefix: String, dir: Path): Path =
        findLatestMatchingFile(prefix, dir) ?: throw FileByPrefixNotFoundException(prefix, dir)

    private suspend fun findLatestMatchingFile(
        prefix: String,
        dir: Path
    ): Path? = withContext(Dispatchers.IO) {
        Files.list(dir).use { stream ->
            stream.asSequence()
                .filter { it.isRegularFile() && it.name.startsWith(prefix) }
                .map { it to runCatching { Files.getLastModifiedTime(it) }.getOrDefault(FileTime.fromMillis(0)) }
                .maxByOrNull { it.second.toMillis() }
                ?.first
        }
    }
}
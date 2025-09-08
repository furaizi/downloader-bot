package com.download.downloaderbot.core.downloader.tools.util.filefinder

import com.download.downloaderbot.core.downloader.tools.ForYtDlp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.isRegularFile
import kotlin.io.path.name
import kotlin.streams.asSequence
import kotlin.use

private val log = KotlinLogging.logger {}

@Component
@ForYtDlp // temporary, can be reused later
class DefaultFilesByPrefixFinder : FilesByPrefixFinder {

    override suspend fun find(prefix: String, dir: Path): List<Path> {
        val file = findFirstMatchingFile(prefix, dir)
            ?: throw FilesByPrefixNotFoundException(prefix, dir)
        log.info { "File found with prefix '$prefix' in directory '$dir': $file" }
        return listOf(file)
    }

    private suspend fun findFirstMatchingFile(
        prefix: String,
        dir: Path
    ): Path? = withContext(Dispatchers.IO) {
        Files.list(dir).use { stream ->
            stream.asSequence()
                .filter { it.isRegularFile() && it.name.startsWith(prefix) }
                .firstOrNull()
        }
    }

}
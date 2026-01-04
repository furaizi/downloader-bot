package com.download.downloaderbot.infra.media.files

import com.download.downloaderbot.infra.di.ForInstaloader
import com.download.downloaderbot.infra.di.ForYtDlp
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
@ForYtDlp
@ForInstaloader
class SingleFileByPrefixFinder : FilesByPrefixFinder {

    override suspend fun find(prefix: String, dir: Path): List<Path> {
        val file = findFirstMatchingFile(prefix, dir)
        return if (file == null) {
            log.info { "No file found with prefix '$prefix' in directory '$dir'" }
            emptyList()
        } else {
            log.debug { "File found with prefix '$prefix' in directory '$dir': $file" }
            listOf(file)
        }
    }

    private suspend fun findFirstMatchingFile(prefix: String, dir: Path): Path? =
        withContext(Dispatchers.IO) {
            Files.list(dir).use { stream ->
                stream.asSequence()
                    .firstOrNull { it.isRegularFile() && it.name.startsWith(prefix) }
            }
        }
}

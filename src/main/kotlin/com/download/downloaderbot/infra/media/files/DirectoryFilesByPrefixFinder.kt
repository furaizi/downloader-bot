package com.download.downloaderbot.infra.media.files

import com.download.downloaderbot.infra.di.ForGalleryDl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.isRegularFile
import kotlin.streams.asSequence

private val log = KotlinLogging.logger {}
private val leadingNumber = Regex("^(\\d+)\\b")

@Component
@ForGalleryDl
class DirectoryFilesByPrefixFinder : FilesByPrefixFinder {
    override suspend fun find(
        prefix: String,
        dir: Path,
    ): List<Path> =
        withContext(Dispatchers.IO) {
            val files = findAllFilesMatchingDirectory(prefix, dir)
            if (files.isEmpty()) {
                log.info { "No files found with directory prefix '$prefix' in directory '$dir'" }
            } else {
                log.info { "Files found with directory prefix '$prefix' in directory '$dir': $files" }
            }
            files
        }

    private fun findAllFilesMatchingDirectory(
        prefix: String,
        dir: Path,
    ): List<Path> {
        val matchingDir =
            Files.list(dir).use { stream ->
                stream.asSequence()
                    .filter { Files.isDirectory(it) }
                    .firstOrNull { it.fileName.toString().startsWith(prefix) }
            } ?: return emptyList()

        return Files.list(matchingDir).use { stream ->
            stream.asSequence()
                .filter { it.isRegularFile() }
                .sortedWith(
                    compareBy<Path> {
                        leadingNumber.find(it.fileName.toString())
                            ?.groupValues?.get(1)
                            ?.toLongOrNull() ?: Long.MAX_VALUE
                    }.thenBy { it.fileName.toString() },
                )
                .toList()
        }
    }
}

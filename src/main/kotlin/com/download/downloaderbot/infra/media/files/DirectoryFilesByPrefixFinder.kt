package com.download.downloaderbot.infra.media.files

import com.download.downloaderbot.infra.di.ForGalleryDl
import mu.KotlinLogging
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.isRegularFile
import kotlin.streams.asSequence

private val log = KotlinLogging.logger {}

@Component
@ForGalleryDl
class DirectoryFilesByPrefixFinder : FilesByPrefixFinder {
    override suspend fun find(
        prefix: String,
        dir: Path,
    ): List<Path> {
        val files = findAllFilesMatchingDirectory(prefix, dir)
        if (files.isEmpty()) {
            throw FilesByDirectoryPrefixNotFoundException(prefix, dir)
        }
        log.info { "Files found with directory prefix '$prefix' in directory '$dir': $files" }
        return files
    }

    private suspend fun findAllFilesMatchingDirectory(
        prefix: String,
        dir: Path,
    ): List<Path> {
        val matchingDir =
            Files.list(dir).use { stream ->
                stream.asSequence()
                    .filter { Files.isDirectory(it) && it.fileName.toString().startsWith(prefix) }
                    .first()
            }

        return Files.list(matchingDir).use { stream ->
            stream.asSequence()
                .filter { it.isRegularFile() }
                .sortedWith(
                    compareBy<Path> { it.fileName.toString() },
                )
                .toList()
        }
    }
}

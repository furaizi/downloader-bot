package com.download.downloaderbot.infra.media.files

import java.nio.file.Path

open class FilesByPrefixFinderException(message: String) : RuntimeException(message)

class FilesByPrefixNotFoundException(
    val prefix: String,
    val dir: Path
) : FilesByPrefixFinderException("File with prefix '$prefix' not found in directory: $dir")

class FilesByDirectoryPrefixNotFoundException(
    val prefix: String,
    val dir: Path
) : FilesByPrefixFinderException("Directory with prefix '$prefix' not found in directory: $dir")
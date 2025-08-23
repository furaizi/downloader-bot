package com.download.downloaderbot.core.tools.util.filefinder

import java.nio.file.Path

open class FileByPrefixFinderException(message: String) : RuntimeException(message)

class FileByPrefixNotFoundException(
    val prefix: String,
    val dir: Path
) : FileByPrefixFinderException("File with prefix '$prefix' not found in directory: $dir")
package com.download.downloaderbot.bot.gateway

import com.download.downloaderbot.core.domain.Media
import java.io.File

sealed class InputFile {
    data class Local(val file: File) : InputFile()

    data class Id(val fileId: String) : InputFile()
}

fun File.asInputFile(): InputFile = InputFile.Local(this)

fun String.asInputFile(): InputFile = InputFile.Id(this)

fun Media.toInputFile(): InputFile =
    lastFileId?.asInputFile()
        ?: File(fileUrl).asInputFile()

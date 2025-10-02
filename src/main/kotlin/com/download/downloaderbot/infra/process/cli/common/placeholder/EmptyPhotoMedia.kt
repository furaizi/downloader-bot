package com.download.downloaderbot.infra.process.cli.common.placeholder

import com.download.downloaderbot.core.domain.Media
import com.download.downloaderbot.core.domain.MediaType
import com.download.downloaderbot.infra.process.cli.api.MediaConvertible
import java.nio.file.Path

data class EmptyPhotoMedia(val title: String = "") : MediaConvertible {
    override fun toMedia(
        filePath: Path,
        sourceUrl: String,
    ) = Media(
        type = MediaType.IMAGE,
        fileUrl = filePath.toAbsolutePath().toString(),
        sourceUrl = sourceUrl,
        title = this.title,
    )
}

data class EmptyVideoMedia(val title: String = "") : MediaConvertible {
    override fun toMedia(
        filePath: Path,
        sourceUrl: String,
    ) = Media(
        type = MediaType.VIDEO,
        fileUrl = filePath.toAbsolutePath().toString(),
        sourceUrl = sourceUrl,
        title = this.title,
    )
}

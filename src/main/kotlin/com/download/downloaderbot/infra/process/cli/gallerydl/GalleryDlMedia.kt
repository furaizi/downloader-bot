package com.download.downloaderbot.infra.process.cli.gallerydl

import com.download.downloaderbot.core.domain.Media
import com.download.downloaderbot.core.domain.MediaType
import com.download.downloaderbot.infra.process.cli.api.MediaConvertible
import java.nio.file.Path

data class GalleryDlMedia(val title: String = "") : MediaConvertible {
    override fun toMedia(
        filePath: Path,
        sourceUrl: String,
    ) = Media(
        type = MediaType.IMAGE,
        fileUrl = filePath.toAbsolutePath().toString(),
        sourceUrl = sourceUrl,
        title = this.title,
    )

    override fun mediaType() = MediaType.IMAGE
}

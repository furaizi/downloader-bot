package com.download.downloaderbot.infra.providers.util

import com.download.downloaderbot.core.domain.Media
import com.download.downloaderbot.core.domain.MediaType
import com.download.downloaderbot.infra.process.cli.gallerydl.GalleryDlMedia
import com.download.downloaderbot.infra.process.cli.instaloader.InstaloaderMedia
import com.download.downloaderbot.infra.process.cli.ytdlp.YtDlpMedia
import java.nio.file.Path

fun YtDlpMedia.toMedia(filePath: Path, sourceUrl: String) = Media(
    type = MediaType.fromString(this.type),
    fileUrl = filePath.toAbsolutePath().toString(),
    sourceUrl = sourceUrl,
    title = this.title
)

fun GalleryDlMedia.toMedia(filePath: Path, sourceUrl: String) = Media(
    type = MediaType.IMAGE,
    fileUrl = filePath.toAbsolutePath().toString(),
    sourceUrl = sourceUrl,
    title = this.title
)

fun InstaloaderMedia.toMedia(filePath: Path, sourceUrl: String) = Media(
    type = MediaType.VIDEO,
    fileUrl = filePath.toAbsolutePath().toString(),
    sourceUrl = sourceUrl,
    title = this.node.title,
)


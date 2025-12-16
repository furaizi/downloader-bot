package com.download.downloaderbot.infra.process.cli.base

import com.download.downloaderbot.app.config.properties.MediaProperties
import com.download.downloaderbot.app.config.properties.MediaSizeLimits
import com.download.downloaderbot.core.domain.MediaType
import com.download.downloaderbot.core.downloader.MediaTooLargeException
import com.download.downloaderbot.infra.config.tools.CliToolFixture
import com.download.downloaderbot.infra.process.cli.common.ShellGalleryDlCommandBuilder
import com.download.downloaderbot.infra.process.cli.common.ShellYtDlpCommandBuilder
import com.download.downloaderbot.infra.process.utils.PosixShellCondition
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.annotation.EnabledIf
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldStartWith
import io.kotest.matchers.paths.shouldExist
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldEndWith
import org.springframework.util.unit.DataSize
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name

@EnabledIf(PosixShellCondition::class)
class BaseCliToolIT : FunSpec({

    suspend fun withTempEnvironment(
        maxVideoSize: DataSize? = null,
        testBlock: suspend (Path, CliToolFixture) -> Unit,
    ) {
        val tempDir = Files.createTempDirectory("cli-tool-it-")
        try {
            val limits =
                maxVideoSize?.let { MediaSizeLimits(video = it) }
                    ?: MediaSizeLimits()
            val props = MediaProperties(tempDir.toString(), maxSize = limits)
            val fixture = CliToolFixture(props)

            testBlock(tempDir, fixture)
        } finally {
            tempDir.toFile().deleteRecursively()
        }
    }

    test("yt-dlp. happy path: probe -> size ok -> download -> returns Media mapped from parsed metadata") {
        withTempEnvironment { dir, factory ->
            val url = "https://example.com/video"

            val cmd =
                ShellYtDlpCommandBuilder()
                    .probeOk(ytDlpJson(title = "My video", type = "video", filesize = 1024))
                    .downloadCreatesFiles(exts = listOf("mp4"))
            val ytDlp = factory.ytDlp(cmd)

            val result = ytDlp.download(url, formatOverride = "best")

            result shouldHaveSize 1

            val media = result.single()
            assertSoftly(media) {
                type shouldBe MediaType.VIDEO
                sourceUrl shouldBe url
                title shouldBe "My video"
            }

            assertSoftly(Path.of(media.fileUrl)) {
                shouldExist()
                shouldStartWith(dir)
                name shouldEndWith ".mp4"
            }
        }
    }

    test("yt-dlp. estimated size too large -> throws and does not create output") {
        withTempEnvironment(maxVideoSize = DataSize.ofBytes(1)) { dir, factory ->
            val url = "https://example.com/video"
            val cmd =
                ShellYtDlpCommandBuilder()
                    .probeOk(ytDlpJson(title = "Big", type = "video", filesize = 2))
                    .downloadCreatesFiles(exts = listOf("mp4"))
            val ytDlp = factory.ytDlp(cmd)

            shouldThrow<MediaTooLargeException> {
                ytDlp.download(url, formatOverride = "")
            }

            dir.listDirectoryEntries().shouldBeEmpty()
        }
    }

    test("gallery-dl. directory download -> DirectoryFilesByPrefixFinder returns sorted files -> Media for each file") {
        withTempEnvironment { dir, factory ->
            val url = "https://example.com/gallery"
            val createdFiles = listOf("10.jpg", "2.jpg", "1.jpg", "abc.jpg")
            val expectedOrder = listOf("1.jpg", "2.jpg", "10.jpg", "abc.jpg")

            val cmd =
                ShellGalleryDlCommandBuilder()
                    .probeFails()
                    .downloadCreatesDirWithFiles(fileNames = createdFiles)
            val galleryDl = factory.galleryDl(cmd)

            val result = galleryDl.download(url, formatOverride = "")

            result.shouldHaveSize(4)
            result.all { it.type == MediaType.IMAGE } shouldBe true

            val paths = result.map { Paths.get(it.fileUrl) }
            assertSoftly(paths) {
                forEach {
                    it.shouldExist()
                    it shouldStartWith dir
                }
                map { it.name } shouldContainExactly expectedOrder
                map { it.parent }.distinct() shouldHaveSize 1
            }
        }
    }
})

private fun ytDlpJson(
    title: String,
    type: String,
    filesize: Long,
): String =
    """
    {
      "title": "$title",
      "filename": "ignored",
      "resolution": "1920x1080",
      "duration": 10,
      "width": 1920,
      "height": 1080,
      "filesize": $filesize,
      "filesize_approx": 0,
      "extractor": "unit",
      "uploader": "unit",
      "_type": "$type",
      "ext": "mp4",
      "hasAudio": true,
      "vcodec": "h264",
      "acodec": "aac",
      "tbr": 1000.0
    }
    """.trimIndent()
        .lines()
        .joinToString("") { it.trim() }

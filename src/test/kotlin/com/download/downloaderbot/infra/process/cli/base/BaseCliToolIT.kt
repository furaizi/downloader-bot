package com.download.downloaderbot.infra.process.cli.base

import com.download.downloaderbot.app.config.properties.MediaProperties
import com.download.downloaderbot.app.config.properties.MediaSizeLimits
import com.download.downloaderbot.core.domain.MediaType
import com.download.downloaderbot.core.downloader.MediaTooLargeException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.springframework.util.unit.DataSize
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.exists

class BaseCliToolIT : FunSpec({

    val enabled = isPosixShellAvailable()

    suspend fun withTempDir(block: suspend (Path) -> Unit) {
        val dir = Files.createTempDirectory("base-cli-tool-it-")
        try {
            block(dir)
        } finally {
            dir.toFile().deleteRecursively()
        }
    }

    test("yt-dlp. happy path: probe -> size ok -> download -> returns Media mapped from parsed metadata")
        .config(enabled = enabled) {
            withTempDir { dir ->
                val url = "https://example.com/video"
                val props = MediaProperties(dir.toString())

                val cmd = ShellYtDlpCommandBuilder()
                    .probeOk(ytDlpJson(title = "My video", type = "video", filesize = 1024))
                    .downloadCreatesFiles(exts = listOf("mp4"))
                val factory = CliToolFixture(props)
                val tool = factory.ytDlp(cmd)

                val res = tool.download(url, formatOverride = "best")

                res.shouldHaveSize(1)

                val media = res.single()
                media.apply {
                    type shouldBe MediaType.VIDEO
                    sourceUrl shouldBe url
                    title shouldBe "My video"
                }

                val filePath = Paths.get(media.fileUrl)
                filePath.apply {
                    exists().shouldBeTrue()
                    startsWith(props.basePath).shouldBeTrue()
                    fileName.toString().endsWith(".mp4").shouldBeTrue()
                }
            }

        }

    test("yt-dlp. estimated size too large -> throws and does not create output")
        .config(enabled = enabled) {
            withTempDir { dir ->
                val props = MediaProperties(dir.toString(), maxSize = MediaSizeLimits(video = DataSize.ofBytes(1)))
                val url = "https://example.com/video"

                val cmd = ShellYtDlpCommandBuilder()
                    .probeOk(ytDlpJson(title = "Big", type = "video", filesize = 2))
                    .downloadCreatesFiles(exts = listOf("mp4"))
                val factory = CliToolFixture(props)
                val tool = factory.ytDlp(cmd)

                shouldThrow<MediaTooLargeException> {
                    tool.download(url, formatOverride = "")
                }

                Files.list(props.basePath).use { stream ->
                    stream.count() shouldBe 0L
                }
            }
        }

    test("gallery-dl. directory download -> DirectoryFilesByPrefixFinder returns sorted files -> Media for each file")
        .config(enabled = enabled) {
            withTempDir { dir ->
                val props = MediaProperties(dir.toString())
                val url = "https://example.com/gallery"
                val created = listOf("10.jpg", "2.jpg", "1.jpg", "abc.jpg")
                val expectedOrder = listOf("1.jpg", "2.jpg", "10.jpg", "abc.jpg")

                val cmd = ShellGalleryDlCommandBuilder()
                    .probeFails()
                    .downloadCreatesDirWithFiles(fileNames = created)
                val factory = CliToolFixture(props)
                val tool = factory.galleryDl(cmd)

                val res = tool.download(url, formatOverride = "")

                res.shouldHaveSize(4)
                res.map { it.type }.distinct().single() shouldBe MediaType.IMAGE

                val paths = res.map { Paths.get(it.fileUrl) }

                paths.forEach { p ->
                    p.exists().shouldBeTrue()
                    p.startsWith(props.basePath).shouldBeTrue()
                }

                paths.map { it.fileName.toString() } shouldContainExactly expectedOrder
                paths.map { it.parent }.distinct().size shouldBe 1
            }
        }

})

private fun ytDlpJson(title: String, type: String, filesize: Long): String =
    """{"title":"$title","filename":"ignored","resolution":"1920x1080","duration":10,"width":1920,"height":1080,"filesize":$filesize,"filesize_approx":0,"extractor":"unit","uploader":"unit","_type":"$type","ext":"mp4","hasAudio":true,"vcodec":"h264","acodec":"aac","tbr":1000.0}"""


private fun isPosixShellAvailable(): Boolean {
    val os = System.getProperty("os.name").lowercase()
    if (os.contains("win")) return false
    return Files.isExecutable(Path.of("/bin/sh"))
}

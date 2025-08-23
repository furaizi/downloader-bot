package com.download.downloaderbot.core.usecase

import com.download.downloaderbot.core.downloader.MediaDownloader
import com.download.downloaderbot.core.domain.Media
import com.download.downloaderbot.core.domain.MediaType
import com.download.downloaderbot.core.mediainfo.MediaInfoExtractor
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.attribute.FileTime
import kotlin.io.path.exists
import kotlin.io.path.writeText
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class MediaServiceImplIT {

    @TempDir
    lateinit var tmpDir: Path

    private fun service(
        downloader: MediaDownloader,
        extractor: MediaInfoExtractor,
        downloadsRoot: Path = tmpDir.resolve("downloader-bot-root")
    ) = MediaServiceImpl(downloader, extractor, downloadsRoot)

    private fun createFilesForTemplate(outputTemplate: String, vararg exts: String): List<Path> {
        val parent = Paths.get(outputTemplate).parent
        Files.createDirectories(parent)
        return exts.map { ext ->
            val p = Paths.get(outputTemplate.replace("%(ext)s", ext))
            if (!p.exists()) Files.createFile(p) else p
            p.writeText("dummy")
            p
        }
    }

    @Test
    fun `success flow - creates dir, finds file, passes absolute path to extractor`() = runTest {
        val url = "https://video.example/abc"
        val downloader = mockk<MediaDownloader>()
        val extractor = mockk<MediaInfoExtractor>()

        coEvery { downloader.download(url, any()) } coAnswers {
            val tpl = arg<String>(1)
            val file = createFilesForTemplate(tpl, "mp4").single()
            assertTrue(file.exists())
        }

        coEvery { extractor.fetchMediaInfo(url, any()) } answers {
            val pathArg = arg<String>(1)
            Media(
                type = MediaType.VIDEO,
                fileUrl = pathArg,
                sourceUrl = url,
                title = "OK"
            )
        }

        val downloadsRoot = tmpDir.resolve("root1")
        val svc = service(downloader, extractor, downloadsRoot)

        val media = svc.download(url)

        assertEquals(url, media.sourceUrl)
        assertTrue(downloadsRoot.exists())
        assertTrue(media.fileUrl.endsWith(".mp4"))
        assertTrue(Paths.get(media.fileUrl).isAbsolute)

        coVerify(exactly = 1) {
            downloader.download(
                url,
                match { p -> Paths.get(p).parent == downloadsRoot && p.endsWith(".%(ext)s") }
            )
        }
        coVerify(exactly = 1) { extractor.fetchMediaInfo(url, any()) }
    }

    @Test
    fun `picks latest matching file when multiple exist`() = runTest {
        val url = "https://video.example/multi"
        val downloader = mockk<MediaDownloader>()
        val extractor = mockk<MediaInfoExtractor>()

        lateinit var newer: Path

        coEvery { downloader.download(url, any()) } coAnswers {
            val tpl = arg<String>(1)
            val files = createFilesForTemplate(tpl, "mp4", "webm")
            val older = files[0]
            newer = files[1]
            Files.setLastModifiedTime(older, FileTime.fromMillis(1))
            Files.setLastModifiedTime(newer, FileTime.fromMillis(2))
        }

        coEvery { extractor.fetchMediaInfo(url, any()) } answers {
            val pathArg = arg<String>(1)
            Media(
                type = MediaType.VIDEO,
                fileUrl = pathArg,
                sourceUrl = url,
                title = "Picked-latest"
            )
        }

        val svc = service(downloader, extractor, tmpDir.resolve("root2"))
        val media = svc.download(url)

        assertTrue(media.fileUrl.endsWith(".webm"), "Expected latest file (.webm) to be chosen")
        assertEquals(newer.toAbsolutePath().toString(), media.fileUrl)
    }

    @Test
    fun `wraps downloader errors`() = runTest {
        val url = "https://video.example/fail-dl"
        val downloader = mockk<MediaDownloader>()
        val extractor = mockk<MediaInfoExtractor>()

        coEvery { downloader.download(any(), any()) } throws IllegalStateException("network boom")

        val svc = service(downloader, extractor, tmpDir.resolve("root3"))

        val ex = assertFailsWith<RuntimeException> { svc.download(url) }
        assertTrue(ex.message!!.startsWith("Download failed for url=$url"))
        assertNotNull(ex.cause)
    }

    @Test
    fun `throws when no file created after download`() = runTest {
        val url = "https://video.example/no-file"
        val downloader = mockk<MediaDownloader>()
        val extractor = mockk<MediaInfoExtractor>()

        coEvery { downloader.download(any(), any()) } returns Unit

        val downloadsRoot = tmpDir.resolve("root4")
        val svc = service(downloader, extractor, downloadsRoot)

        val ex = assertFailsWith<RuntimeException> { svc.download(url) }
        assertTrue(ex.message!!.contains("Downloaded file not found for prefix"))
        assertTrue(ex.message!!.contains(downloadsRoot.toString()))
    }

    @Test
    fun `wraps media info extraction errors`() = runTest {
        val url = "https://video.example/fail-meta"
        val downloader = mockk<MediaDownloader>()
        val extractor = mockk<MediaInfoExtractor>()

        coEvery { downloader.download(any(), any()) } coAnswers {
            val tpl = arg<String>(1)
            createFilesForTemplate(tpl, "mp4")
        }

        coEvery { extractor.fetchMediaInfo(any(), any()) } throws IllegalArgumentException("bad json")

        val svc = service(downloader, extractor, tmpDir.resolve("root5"))

        val ex = assertFailsWith<RuntimeException> { svc.download(url) }
        assertEquals("Failed to extract media info", ex.message)
        assertNotNull(ex.cause)
    }
}

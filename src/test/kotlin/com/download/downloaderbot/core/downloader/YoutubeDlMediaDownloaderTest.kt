package com.download.downloaderbot.core.downloader

import com.download.downloaderbot.core.tools.ytdlp.YtDlp
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class YoutubeDlMediaDownloaderTest {

    private val ytDlp: YtDlp = mockk(relaxed = true)
    private val downloader = YoutubeDlMediaDownloader(ytDlp)

    @Test
    fun `delegates to YtDlp with same arguments`() = runTest {
        val url = "https://example.com/video"
        val out = "/tmp/out.%(ext)s"
        coEvery { ytDlp.download(url, out) } returns Unit

        downloader.download(url, out)

        coVerify(exactly = 1) { ytDlp.download(url, out) }
    }

    @Test
    fun `blank url throws IllegalArgumentException`() = runTest {
        assertFailsWith<IllegalArgumentException> {
            downloader.download("", "/tmp/out.%(ext)s")
        }
    }

    @Test
    fun `blank outputPath throws IllegalArgumentException`() = runTest {
        assertFailsWith<IllegalArgumentException> {
            downloader.download("https://example.com/video", "")
        }
    }

    @Test
    fun `exception from YtDlp is propagated`() = runTest {
        val url = "https://example.com/video"
        val out = "/tmp/out.%(ext)s"
        val ex = MediaDownloadException("boom", exitCode = 2, output = "stderr")
        coEvery { ytDlp.download(url, out) } throws ex

        assertFailsWith<MediaDownloadException> {
            downloader.download(url, out)
        }
        coVerify(exactly = 1) { ytDlp.download(url, out) }
    }

    @Test
    fun `cancellation propagates to caller`() = runTest {
        coEvery { ytDlp.download(any(), any()) } coAnswers {
            suspendCancellableCoroutine { /* suspend forever until cancelled */ }
        }

        val job = launch {
            downloader.download("https://example.com/video", "/tmp/out.%(ext)s")
        }

        job.cancel()
        job.join()
        assertTrue(job.isCancelled)
    }
}
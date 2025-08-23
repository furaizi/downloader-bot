package com.download.downloaderbot.core.mediainfo

import com.download.downloaderbot.core.entity.MediaType
import com.download.downloaderbot.core.ytdlp.YtDlp
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

@OptIn(ExperimentalCoroutinesApi::class)
class YoutubeDlMediaInfoExtractorTest {

    private val ytDlp: YtDlp = mockk(relaxed = true)
    private val extractor = YoutubeDlMediaInfoExtractor(ytDlp)

    @Test
    fun `maps JSON to Media correctly when audio present`() = runTest {
        val url = "https://youtube.com/watch?v=abc123"
        val path = "/tmp/output"
        coEvery { ytDlp.dumpJson(url) } returns sampleJson(acodec = "aac")

        val media = extractor.fetchMediaInfo(url, path)

        assertEquals(url, media.sourceUrl)
        assertEquals(path, media.fileUrl)
        assertEquals("Cool video", media.title)
        assertEquals(MediaType.VIDEO, media.type)

        coVerify(exactly = 1) { ytDlp.dumpJson(url) }
    }

    @Test
    fun `throws IllegalArgumentException on blank url or path`() = runTest {
        assertFailsWith<IllegalArgumentException> {
            extractor.fetchMediaInfo("", "/tmp")
        }

        assertFailsWith<IllegalArgumentException> {
            extractor.fetchMediaInfo("https://x", "   ")
        }
    }

    @Test
    fun `wraps JSON parse failure`() = runTest {
        val url = "https://bad-json"
        coEvery { ytDlp.dumpJson(url) } returns "this is not valid json"

        val ex = assertFailsWith<RuntimeException> {
            extractor.fetchMediaInfo(url, "/tmp")
        }

        assertEquals("Failed to parse yt-dlp output", ex.message)
        assertNotNull(ex.cause)
    }


    private fun sampleJson(acodec: String?): String {
        val acodecJson = when (acodec) {
            null -> "null"
            else -> "\"$acodec\""
        }

        return """
        {
          "url": "https://youtube.com/watch?v=abc123",
          "title": "Cool video",
          "filename": "cool_video.mp4",
          "resolution": "1920x1080",
          "duration": 123,
          "width": 1920,
          "height": 1080,
          "filesize": 1048576,
          "extractor": "youtube",
          "uploader": "SomeChannel",
          "_type": "video",
          "ext": "mp4",
          "vcodec": "h264",
          "acodec": $acodecJson
        }
        """.trimIndent()
    }
}

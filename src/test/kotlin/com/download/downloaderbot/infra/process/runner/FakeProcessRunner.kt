package com.download.downloaderbot.infra.process.runner

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.exists

class FakeProcessRunner(
    private val outputExt: String = "mp4",
    private val jsonLine: String = DEFAULT_JSON_LINE,
) : ProcessRunner {
    override suspend fun run(
        args: List<String>,
        url: String,
    ): String {
        val isProbe = args.any { it == "--dump-json" }

        if (!isProbe) {
            val outputPath = extractOutputPath(args)
            if (outputPath != null) {
                createPlaceholderFile(outputPath)
            }
        }

        return jsonLine
    }

    private fun extractOutputPath(args: List<String>): Path? {
        val idx = args.indexOf("-o")
        val template =
            args.getOrNull(idx + 1)
                ?: return null
        return Path.of(template.replace("%(ext)s", outputExt))
    }

    private fun createPlaceholderFile(path: Path) {
        path.parent?.createDirectories()
        if (!path.exists()) {
            Files.createFile(path)
        }
    }

    private companion object {
        private val DEFAULT_JSON_LINE: String =
            """
            {
              "title": "Test video",
              "filename": "ignored",
              "resolution": "720p",
              "duration": 5,
              "width": 1280,
              "height": 720,
              "filesize": 1024,
              "filesize_approx": 0,
              "extractor": "fake",
              "uploader": "tester",
              "_type": "video",
              "ext": "mp4",
              "hasAudio": true,
              "vcodec": "h264",
              "acodec": "aac",
              "tbr": 1000.0
            }
            """.trimIndent()
                .lines()
                .joinToString("") { it.trim() }
    }
}

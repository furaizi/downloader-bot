package com.download.downloaderbot.infra.media.files

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import java.nio.file.Files
import java.nio.file.Path

class SingleFileByPrefixFinderTest : FunSpec({

    lateinit var tempDir: Path
    val finder = SingleFileByPrefixFinder()

    beforeTest {
        tempDir = Files.createTempDirectory("single-file-prefix-finder-test")
    }

    afterTest {
        tempDir.toFile().deleteRecursively()
    }

    context("find") {

        test("returns single file when exactly one file matches prefix") {
            val matching = tempDir.createFile("video_123.mp4")
            tempDir.createFile("other.txt")

            val result = finder.find("video_", tempDir)

            result shouldBe listOf(matching)
        }

        test("returns exactly one matching file when several files share the same prefix") {
            val firstMatch = tempDir.createFile("prefix_a.txt")
            val secondMatch = tempDir.createFile("prefix_b.txt")
            tempDir.createFile("other.txt")

            val result = finder.find("prefix_", tempDir)

            result.shouldHaveSize(1)
            val returned = result.single()

            returned.fileName.toString().startsWith("prefix_") shouldBe true
            (returned == firstMatch || returned == secondMatch) shouldBe true
        }

        test("ignores directories and files whose names do not start with prefix") {
            tempDir.createDirectory("prefix_dir")
            val matching = tempDir.createFile("prefix_file.dat")
            tempDir.createFile("not_prefix_file.dat")

            val result = finder.find("prefix_", tempDir)

            result shouldBe listOf(matching)
        }

        test("does not search recursively in nested directories") {
            val nested = tempDir.createDirectory("nested")
            nested.createFile("prefix_nested.dat")

            shouldThrow<FilesByPrefixNotFoundException> {
                finder.find("prefix_", tempDir)
            }
        }

        test("throws FilesByPrefixNotFoundException when no file with prefix exists") {
            tempDir.createFile("some_other_file.txt")

            shouldThrow<FilesByPrefixNotFoundException> {
                finder.find("missing_", tempDir)
            }
        }
    }
})


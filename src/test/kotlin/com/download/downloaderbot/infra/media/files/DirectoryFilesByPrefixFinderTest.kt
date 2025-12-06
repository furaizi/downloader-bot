package com.download.downloaderbot.infra.media.files

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import java.nio.file.Files
import java.nio.file.Path

class DirectoryFilesByPrefixFinderTest : FunSpec({

    lateinit var tempDir: Path
    val finder = DirectoryFilesByPrefixFinder()

    beforeTest {
        tempDir = Files.createTempDirectory("directory-files-prefix-finder-test")
    }

    afterTest {
        tempDir.toFile().deleteRecursively()
    }

    context("find") {

        test("returns files from matching directory sorted by leading number then name") {
            val matchingDir = tempDir.createDirectory("gallery_2025")
            val file = matchingDir.createFile("file.txt")
            val file2 = matchingDir.createFile("2-second.txt")
            val file10b = matchingDir.createFile("10-b.txt")
            val file3 = matchingDir.createFile("3-third.txt")
            val file10a = matchingDir.createFile("10-a.txt")
            matchingDir.createDirectory("10-subdir")

            val result = finder.find("gallery_", tempDir)

            result.shouldContainExactly(
                listOf(
                    file2,
                    file3,
                    file10a,
                    file10b,
                    file,
                ),
            )
        }

        test("uses one matching directory when several directories share prefix") {
            val firstDir = tempDir.createDirectory("gallery_a")
            val firstFile = firstDir.createFile("1.txt")

            val secondDir = tempDir.createDirectory("gallery_b")
            val secondFile = secondDir.createFile("1.txt")

            val result = finder.find("gallery_", tempDir)

            val fromFirst = listOf(firstFile)
            val fromSecond = listOf(secondFile)

            listOf(fromFirst, fromSecond).any { it == result } shouldBe true
        }

        test("throws FilesByDirectoryPrefixNotFoundException when matching directory has no regular files") {
            val matchingDir = tempDir.createDirectory("gallery_empty")
            matchingDir.createDirectory("nested")

            shouldThrow<FilesByDirectoryPrefixNotFoundException> {
                finder.find("gallery_", tempDir)
            }
        }
    }
})

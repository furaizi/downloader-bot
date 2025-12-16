package com.download.downloaderbot.infra.cleanup

import com.download.downloaderbot.app.config.properties.MediaCleanupProperties
import com.download.downloaderbot.app.config.properties.MediaProperties
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.longs.shouldBeExactly
import io.kotest.matchers.paths.shouldExist
import io.kotest.matchers.paths.shouldNotExist
import io.kotest.matchers.shouldBe
import org.springframework.util.unit.DataSize
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.FileTime
import java.time.Duration
import java.time.Instant
import kotlin.io.path.notExists

class MediaCleanupServiceIT : FunSpec({

    test("returns empty report when basePath does not exist") {
        withTempDir { temp ->
            val missing = temp.resolve("missing-dir")
            missing.notExists() shouldBe true

            val service =
                MediaCleanupService(
                    MediaProperties(
                        missing.toString(),
                        cleanup =
                            MediaCleanupProperties(
                                Duration.ofDays(7),
                                DataSize.ofBytes(10_000),
                            ),
                    ),
                )

            val report = service.cleanup()

            assertSoftly(report) {
                deletedFiles shouldBe 0
                freedBytes shouldBeExactly 0L
            }
        }
    }

    test("throws when maxAge is negative") {
        withTempDir { temp ->
            val service =
                MediaCleanupService(
                    MediaProperties(
                        temp.toString(),
                        cleanup =
                            MediaCleanupProperties(
                                Duration.ofSeconds(-1),
                                DataSize.ofBytes(10_000),
                            ),
                    ),
                )

            shouldThrow<IllegalArgumentException> {
                service.cleanup()
            }
        }
    }

    test("deletes only expired files when maxAge is set and maxTotalSize is large") {
        withTempDir { temp ->
            val now = Instant.now()
            val expired1 = createFile(temp, "expired-1.bin", 120, now.minus(Duration.ofDays(10)))
            val expired2 = createFile(temp, "expired-2.bin", 80, now.minus(Duration.ofDays(8)))
            val fresh = createFile(temp, "fresh.bin", 200, now.minus(Duration.ofHours(1)))

            val service =
                MediaCleanupService(
                    MediaProperties(
                        temp.toString(),
                        cleanup =
                            MediaCleanupProperties(
                                Duration.ofDays(7),
                                DataSize.ofBytes(10_000),
                            ),
                    ),
                )

            val report = service.cleanup()

            expired1.shouldNotExist()
            expired2.shouldNotExist()
            fresh.shouldExist()

            assertSoftly(report) {
                deletedFiles shouldBe 2
                freedBytes shouldBeExactly (120L + 80L)
            }
        }
    }

    test("does not delete by age when maxAge is zero") {
        withTempDir { temp ->
            val now = Instant.now()
            val old = createFile(temp, "old.bin", 100, now.minus(Duration.ofDays(100)))
            val another = createFile(temp, "another.bin", 50, now.minus(Duration.ofDays(50)))

            val service =
                MediaCleanupService(
                    MediaProperties(
                        temp.toString(),
                        cleanup =
                            MediaCleanupProperties(
                                Duration.ZERO,
                                DataSize.ofBytes(10_000),
                            ),
                    ),
                )

            val report = service.cleanup()

            old.shouldExist()
            another.shouldExist()

            assertSoftly(report) {
                deletedFiles shouldBe 0
                freedBytes shouldBeExactly 0L
            }
        }
    }

    test("trims oldest files until total size fits maxTotalSize") {
        withTempDir { temp ->
            val now = Instant.now()
            val oldest = createFile(temp, "a-oldest.bin", 100, now.minus(Duration.ofHours(3)))
            val middle = createFile(temp, "b-middle.bin", 100, now.minus(Duration.ofHours(2)))
            val newest = createFile(temp, "c-newest.bin", 100, now.minus(Duration.ofHours(1)))

            val service =
                MediaCleanupService(
                    MediaProperties(
                        temp.toString(),
                        cleanup =
                            MediaCleanupProperties(
                                Duration.ZERO,
                                DataSize.ofBytes(150),
                            ),
                    ),
                )

            val report = service.cleanup()

            oldest.shouldNotExist()
            middle.shouldNotExist()
            newest.shouldExist()

            assertSoftly(report) {
                deletedFiles shouldBe 2
                freedBytes shouldBeExactly 200L
            }
        }
    }

    test("applies expiry first, then trims remaining files") {
        withTempDir { temp ->
            val now = Instant.now()
            val expired = createFile(temp, "expired.bin", 70, now.minus(Duration.ofDays(30)))
            val remainOldest = createFile(temp, "remain-oldest.bin", 100, now.minus(Duration.ofHours(5)))
            val remainNewest = createFile(temp, "remain-newest.bin", 120, now.minus(Duration.ofHours(1)))

            val service =
                MediaCleanupService(
                    MediaProperties(
                        temp.toString(),
                        cleanup =
                            MediaCleanupProperties(
                                Duration.ofDays(7),
                                DataSize.ofBytes(120),
                            ),
                    ),
                )

            val report = service.cleanup()

            expired.shouldNotExist()
            remainOldest.shouldNotExist()
            remainNewest.shouldExist()

            assertSoftly(report) {
                deletedFiles shouldBe 2
                freedBytes shouldBeExactly (70L + 100L)
            }
        }
    }

    test("cleans up nested regular files") {
        withTempDir { temp ->
            val now = Instant.now()
            val subDir = Files.createDirectories(temp.resolve("sub"))
            val expired = createFile(subDir, "nested-expired.bin", 40, now.minus(Duration.ofDays(20)))
            val fresh = createFile(subDir, "nested-fresh.bin", 60, now.minus(Duration.ofHours(2)))

            val service =
                MediaCleanupService(
                    MediaProperties(
                        temp.toString(),
                        cleanup =
                            MediaCleanupProperties(
                                Duration.ofDays(7),
                                DataSize.ofBytes(10_000),
                            ),
                    ),
                )

            val report = service.cleanup()

            expired.shouldNotExist()
            fresh.shouldExist()

            assertSoftly(report) {
                deletedFiles shouldBe 1
                freedBytes shouldBeExactly 40L
            }
        }
    }
})

private fun createFile(
    dir: Path,
    name: String,
    sizeBytes: Int,
    lastModified: Instant,
): Path {
    val path = dir.resolve(name)
    Files.write(path, ByteArray(sizeBytes))
    Files.setLastModifiedTime(path, FileTime.from(lastModified))
    Files.size(path) shouldBeExactly sizeBytes.toLong()
    return path
}

private suspend fun withTempDir(block: suspend (Path) -> Unit) {
    val dir = Files.createTempDirectory("media-cleanup-it-")
    try {
        block(dir)
    } finally {
        dir.toFile().deleteRecursively()
    }
}

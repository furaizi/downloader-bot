package com.download.downloaderbot.infra.source

import com.download.downloaderbot.app.config.properties.SourcesProperties
import org.openjdk.jmh.annotations.*
import org.openjdk.jmh.infra.Blackhole
import java.util.concurrent.TimeUnit

@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(2)
open class SourceRegistryBenchmark {

    private lateinit var registry: SourceRegistry

    private val firstMatchUrl = "https://www.youtube.com/watch?v=abcdefghijk"
    private val lastMatchUrl = "https://tail.benchmark.example/service/slow-path-12345"
    private val unsupportedUrl = "https://unsupported.benchmark.example/nothing/here"

    @Setup(Level.Trial)
    fun setUp() {
        registry = SourceRegistry(buildProperties())
    }

    @Benchmark
    fun matchAtStart(bh: Blackhole) {
        bh.consume(registry.match(firstMatchUrl))
    }

    @Benchmark
    fun matchAtEnd(bh: Blackhole) {
        bh.consume(registry.match(lastMatchUrl))
    }

    @Benchmark
    fun matchUnsupported(bh: Blackhole) {
        bh.consume(registry.match(unsupportedUrl))
    }

    private fun buildProperties(): SourcesProperties =
        sourcesProps {
            source("youtube") {
                sub(
                    name = "videos",
                    tool = "yt-dlp",
                    format = "mp4",
                    patterns = listOf(
                        "youtube\\.com/watch\\?v=[A-Za-z0-9_-]{11}",
                        "youtu\\.be/[A-Za-z0-9_-]{11}",
                    ),
                )
                sub(
                    name = "shorts",
                    tool = "yt-dlp",
                    format = "mp4",
                    patterns = listOf(
                        "youtube\\.com/shorts/[A-Za-z0-9_-]{11}",
                    ),
                )
            }

            repeat(FILLER_SOURCE_COUNT) { index ->
                fillerSource(index)
            }

            source("tail-end") {
                sub(
                    name = "items",
                    tool = "gallery-dl",
                    format = "raw",
                    patterns = listOf(
                        "tail\\.benchmark\\.example/service/[A-Za-z0-9_-]+",
                    ),
                )
            }
        }

    private fun SourcesBuilder.fillerSource(index: Int) {
        val domain = "service$index.benchmark.example"
        source("placeholder-$index") {
            sub(
                name = "posts",
                tool = "placeholder-tool",
                format = "json",
                patterns = listOf(
                    "$domain/posts/[A-Za-z0-9_-]{4,}",
                    "$domain/p/[A-Za-z0-9]{6}",
                ),
            )
            sub(
                name = "media",
                tool = "placeholder-tool",
                format = "mp4",
                patterns = listOf(
                    "cdn$index\\.benchmark\\.example/stream/[A-Za-z0-9_-]{10}",
                ),
            )
        }
    }

    private companion object {
        const val FILLER_SOURCE_COUNT = 20
    }
}

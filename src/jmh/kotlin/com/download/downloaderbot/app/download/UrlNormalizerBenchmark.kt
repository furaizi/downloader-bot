package com.download.downloaderbot.app.download

import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.BenchmarkMode
import org.openjdk.jmh.annotations.Fork
import org.openjdk.jmh.annotations.Level
import org.openjdk.jmh.annotations.Measurement
import org.openjdk.jmh.annotations.Mode
import org.openjdk.jmh.annotations.OutputTimeUnit
import org.openjdk.jmh.annotations.Param
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.Setup
import org.openjdk.jmh.annotations.State
import org.openjdk.jmh.annotations.Warmup
import org.openjdk.jmh.infra.Blackhole
import java.util.concurrent.TimeUnit

@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
open class UrlNormalizerBenchmark {
    private lateinit var normalizer: UrlNormalizer

    @Param("25", "75")
    var utmCount: Int = 0

    private lateinit var dirtyYoutubeUrl: String

    @Setup(Level.Trial)
    fun setUp() {
        normalizer = UrlNormalizer()
        dirtyYoutubeUrl = buildDirtyYoutubeUrl(utmCount)
    }

    @Benchmark
    fun normalizeDirtyYoutubeUrl(blackhole: Blackhole) {
        blackhole.consume(normalizer.normalize(dirtyYoutubeUrl))
    }

    private fun buildDirtyYoutubeUrl(utmCount: Int): String {
        val builder =
            StringBuilder("https://www.youtube.com/watch?")
                .append("v=abcdefghijk")
                .append("&list=PL1234567890")
                .append("&index=4")
                .append("&feature=share")
                .append("&app=desktop")
                .append("&fbclid=fbclid-value-123")
                .append("&gclid=gclid-value-456")
                .append("&msclkid=msclkid-value-789")
                .append("&igshid=igshid-value-000")
                .append("&ref=telegram")
                .append("&si=signature-value")

        // Add many UTM parameters to stress parsing/cleaning.
        repeat(utmCount) { i ->
            builder.append("&utm_source_$i=newsletter%20").append(i)
            builder.append("&utm_medium=social%2Ftelegram")
            builder.append("&utm_campaign=spring_sale_").append(i)
            builder.append("&utm_content=cta_button_").append(i)
        }

        return builder.toString()
    }
}

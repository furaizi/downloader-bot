package com.download.downloaderbot.e2e

import com.download.downloaderbot.app.config.properties.MediaProperties
import com.download.downloaderbot.bot.commands.util.updateDownload
import com.download.downloaderbot.bot.core.UpdateHandler
import com.download.downloaderbot.bot.exception.BotErrorGuard
import com.download.downloaderbot.bot.gateway.RecordingBotPort
import com.download.downloaderbot.core.cache.CachePort
import com.download.downloaderbot.core.domain.Media
import com.download.downloaderbot.app.download.UrlNormalizer
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.longs.shouldBeLessThan
import org.springframework.test.context.TestPropertySource
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@DownloaderBotE2E
@TestPropertySource(properties = ["downloader.yt-dlp.runner=sleepy"])
class ThroughputE2E(
    updateHandler: UpdateHandler,
    botPort: RecordingBotPort,
    mediaProps: MediaProperties,
    errorGuard: BotErrorGuard,
) : AbstractE2E(
    updateHandler,
    botPort,
    mediaProps,
    errorGuard,
    body = {

        test("processes 500 download jobs with 5 workers within reasonable time") {
            val jobs = 500
            val chatId = 888L
            val start = System.nanoTime()

            repeat(jobs) { idx ->
                val url = "https://example.com/video/$idx-${Random.nextLong()}"
                handle(updateDownload(url, chatId, idx.toLong(), idx.toLong()))
            }

            eventually(60.seconds) {
                botPort.sentMedia.shouldHaveSize(jobs)
            }

            val elapsedMs = (System.nanoTime() - start)
                .toDuration(DurationUnit.NANOSECONDS)
                .inWholeMilliseconds
            // Each job does probe+download (~2 * 100ms) across 5 workers => ~20s baseline. Keep budget generous.
            elapsedMs.shouldBeLessThan(60_000)
        }
    },
)

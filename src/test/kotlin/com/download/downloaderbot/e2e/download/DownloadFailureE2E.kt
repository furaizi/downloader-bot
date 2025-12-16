package com.download.downloaderbot.e2e.download

import com.download.downloaderbot.app.config.properties.MediaProperties
import com.download.downloaderbot.app.download.UrlNormalizer
import com.download.downloaderbot.bot.commands.util.updateDownload
import com.download.downloaderbot.bot.core.UpdateHandler
import com.download.downloaderbot.bot.exception.BotErrorGuard
import com.download.downloaderbot.bot.gateway.RecordingBotPort
import com.download.downloaderbot.core.cache.CachePort
import com.download.downloaderbot.core.domain.Media
import com.download.downloaderbot.core.lock.UrlLockManager
import com.download.downloaderbot.e2e.config.AbstractE2E
import com.download.downloaderbot.e2e.config.DownloaderBotE2E
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.springframework.test.context.TestPropertySource
import java.time.Duration
import kotlin.time.Duration.Companion.seconds

@Suppress("LongParameterList")
@DownloaderBotE2E
@TestPropertySource(properties = ["downloader.yt-dlp.runner=fail"])
class DownloadFailureE2E(
    updateHandler: UpdateHandler,
    botPort: RecordingBotPort,
    mediaProps: MediaProperties,
    errorGuard: BotErrorGuard,
    private val urlLock: UrlLockManager,
    private val cache: CachePort<String, List<Media>>,
    private val normalizer: UrlNormalizer,
) : AbstractE2E(
        updateHandler,
        botPort,
        mediaProps,
        errorGuard,
        body = {

            test("releases url lock and notifies user when download fails") {
                val url = "https://example.com/magic/fail"
                val chatId = 303L
                val failureText = "Внутрішній інструмент не зміг виконатися."
                val key = normalizer.normalize(url)

                cache.evict(key)
                urlLock.tryAcquire(url, Duration.ofSeconds(1))?.let { urlLock.release(url, it) }

                val firstMessageId = 1L
                handle(updateDownload(url, chatId, firstMessageId))

                eventually(5.seconds) {
                    botPort.sentTexts.shouldHaveSize(1)
                    assertSoftly(botPort.sentTexts.first()) {
                        this.chatId shouldBe chatId
                        replyToMessageId shouldBe firstMessageId
                        text shouldBe failureText
                    }
                }

                eventually(2.seconds) {
                    val token = urlLock.tryAcquire(url, Duration.ofSeconds(1)).shouldNotBeNull()
                    urlLock.release(url, token)
                    cache.get(key) shouldBe null
                }

                val secondMessageId = 2L
                handle(updateDownload(url, chatId, secondMessageId))

                eventually(5.seconds) {
                    botPort.sentTexts.shouldHaveSize(2)
                    assertSoftly(botPort.sentTexts.last()) {
                        this.chatId shouldBe chatId
                        replyToMessageId shouldBe secondMessageId
                        text shouldBe failureText
                    }
                }
            }
        },
    )
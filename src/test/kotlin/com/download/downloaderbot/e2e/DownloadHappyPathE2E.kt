package com.download.downloaderbot.e2e

import com.download.downloaderbot.app.config.properties.MediaProperties
import com.download.downloaderbot.app.download.UrlNormalizer
import com.download.downloaderbot.bot.commands.util.updateDownload
import com.download.downloaderbot.bot.core.UpdateHandler
import com.download.downloaderbot.bot.gateway.InputFile
import com.download.downloaderbot.bot.gateway.RecordingBotPort
import com.download.downloaderbot.bot.gateway.telegram.fileId
import com.download.downloaderbot.core.cache.CachePort
import com.download.downloaderbot.core.domain.Media
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.file.shouldExist
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.paths.shouldExist
import io.kotest.matchers.shouldBe
import org.springframework.test.context.TestPropertySource
import java.nio.file.Path
import kotlin.time.Duration.Companion.seconds

@DownloaderBotE2E
@TestPropertySource(properties = ["downloader.yt-dlp.runner=fake"])
class DownloadHappyPathE2E(
    updateHandler: UpdateHandler,
    botPort: RecordingBotPort,
    mediaProps: MediaProperties,
    private val cache: CachePort<String, List<Media>>,
    private val normalizer: UrlNormalizer,
) : AbstractE2E(
        updateHandler,
        botPort,
        mediaProps,
        body = {

            test("downloads video via /download command and stores cache") {
                val url = "https://example.com/magic/video-123"
                val chatId = 4242L
                val messageId = 111L

                val key = normalizer.normalize(url)
                cache.evict(key)

                handle(updateDownload(url, chatId, messageId))

                val sent =
                    eventually(5.seconds) {
                        botPort.sentMedia.shouldHaveSize(1)
                        botPort.sentMedia.single()
                    }

                assertSoftly(sent) {
                    this.chatId shouldBe chatId
                    options.replyToMessageId shouldBe messageId
                    (file as InputFile.Local).file.shouldExist()
                }

                val cached =
                    eventually(5.seconds) {
                        assertSoftly(cache.get(key)) {
                            this.shouldNotBeNull()
                            this.shouldHaveSize(1)
                        } as List<Media>
                    }.single()

                cached.lastFileId shouldBe sent.message.fileId
                Path.of(cached.fileUrl).shouldExist()
            }
        },
    )

package com.download.downloaderbot.bot.job

import com.download.downloaderbot.app.config.properties.CacheProperties
import com.download.downloaderbot.app.download.MediaService
import com.download.downloaderbot.bot.commands.util.InputValidator
import com.download.downloaderbot.bot.config.properties.BotIdentity
import com.download.downloaderbot.bot.config.properties.BotProperties
import com.download.downloaderbot.bot.gateway.MediaInput
import com.download.downloaderbot.bot.gateway.MessageOptions
import com.download.downloaderbot.bot.gateway.asInputFile
import com.download.downloaderbot.bot.gateway.telegram.fileId
import com.download.downloaderbot.bot.gateway.telegram.fileUniqueId
import com.download.downloaderbot.bot.gateway.telegram.getOrThrow
import com.download.downloaderbot.bot.gateway.telegram.sendMediaAlbumChunked
import com.download.downloaderbot.bot.gateway.telegram.sendSmartMedia
import com.download.downloaderbot.bot.gateway.telegram.toTelegram
import com.download.downloaderbot.bot.gateway.toInputFile
import com.download.downloaderbot.bot.promo.PromoService
import com.download.downloaderbot.bot.ui.shareKeyboard
import com.download.downloaderbot.core.cache.CachePort
import com.download.downloaderbot.core.domain.Media
import com.download.downloaderbot.core.domain.MediaType
import com.download.downloaderbot.core.downloader.MediaNotFoundException
import com.download.downloaderbot.infra.metrics.BotMetrics
import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.Message
import io.github.oshai.kotlinlogging.KotlinLogging
import io.micrometer.core.instrument.Timer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Component
import java.io.File
import java.time.Duration

private val log = KotlinLogging.logger {}

@Suppress("LongParameterList")
@Component
class DefaultDownloadJobExecutor(
    private val service: MediaService,
    private val bot: Bot,
    private val props: BotProperties,
    private val cachePort: CachePort<String, List<Media>>,
    private val cacheProps: CacheProperties,
    private val promoService: PromoService,
    private val botIdentity: BotIdentity,
    private val botMetrics: BotMetrics,
    private val validator: InputValidator,
) : DownloadJobExecutor {
    private companion object {
        const val TELEGRAM_ALBUM_LIMIT = 10
        const val JOB_NAME = "download"
    }

    private val share by lazy { shareKeyboard(botIdentity.username, props.shareText) }

    override suspend fun execute(job: DownloadJob) {
        measureJob(job) {
            log.info { "Executing download job id=${job.id} url=${job.sourceUrl} chatId=${job.chatId}" }

            val mediaList =
                withContext(Dispatchers.IO) {
                    service.download(job.sourceUrl)
                }

            if (mediaList.isEmpty()) {
                throw MediaNotFoundException(job.sourceUrl)
            }

            val normalizedUrl = mediaList.first().sourceUrl
            val messages = sendMediaSmart(job.chatId, mediaList, job.replyToMessageId)
            updateCache(normalizedUrl, mediaList, messages)

            log.info {
                val titles = mediaList.map { it.title }
                val paths = mediaList.map { it.fileUrl }
                "Downloaded: $titles ($paths)"
            }
        }
    }

    private suspend fun <T> measureJob(
        job: DownloadJob,
        block: suspend () -> T,
    ): T {
        val startNanos = System.nanoTime()
        botMetrics
            .jobQueueDelayTimer(JOB_NAME)
            .record(Duration.ofNanos(startNanos - job.enqueuedAtNanos))

        val executionSample = Timer.start()
        return try {
            block()
        } finally {
            executionSample.stop(botMetrics.jobDurationTimer(JOB_NAME))
            val totalNanos = System.nanoTime() - job.enqueuedAtNanos
            botMetrics
                .jobTotalDurationTimer(JOB_NAME)
                .record(Duration.ofNanos(totalNanos))
        }
    }

    private suspend fun sendMediaSmart(
        chatId: Long,
        mediaList: List<Media>,
        replyTo: Long?,
    ): List<Message> {
        val sendPromo = promoService.shouldSend(chatId)

        return if (mediaList.isVisualAlbum()) {
            val items = mediaList.toMediaInputs()

            try {
                val sentPhotos = sendAlbum(chatId, items, replyTo)
                if (sendPromo && sentPhotos.isNotEmpty()) {
                    bot.sendMessage(
                        chatId = ChatId.fromId(chatId),
                        text = props.promoText,
                        replyToMessageId = sentPhotos.firstOrNull()?.messageId ?: replyTo,
                        replyMarkup = share,
                    ).getOrThrow()
                }
                sentPhotos
            } catch (e: Exception) {
                log.warn(e) { "Failed to send media album" }
                emptyList()
            }
        } else {
            val results =
                mediaList.mapNotNull { media ->
                    try {
                        val input = media.toInputFile()
                        val options =
                            MessageOptions(
                                caption = if (sendPromo) props.promoText else null,
                                replyToMessageId = replyTo,
                                replyMarkup = if (sendPromo) share else null,
                            )
                        bot.sendSmartMedia(
                            type = media.type,
                            chatId = chatId,
                            file = input.toTelegram(),
                            options = options,
                        )
                    } catch (e: Exception) {
                        log.warn(e) { "Failed to send media item" }
                        null
                    }
                }
            results
        }
    }

    private suspend fun sendAlbum(
        chatId: Long,
        items: List<MediaInput>,
        replyTo: Long?
    ): List<Message> = bot.sendMediaAlbumChunked(chatId, items, replyToMessageId = replyTo)

    private suspend fun updateCache(
        url: String,
        mediaList: List<Media>,
        messages: List<Message>,
    ) {
        if (validator.isInstagramStoriesUrl(url)) {
            log.debug { "Skipping cache for Instagram stories url=$url" }
            return
        }

        val updated =
            mediaList
                .zip(messages)
                .map { (media, message) -> media.updateWith(message) }
        if (updated.any { it.lastFileId != null }) {
            cachePort.put(url, updated, cacheProps.mediaTtl)
        }
    }

    private fun Media.updateWith(message: Message): Media =
        copy(
            lastFileId = message.fileId ?: lastFileId,
            fileUniqueId = message.fileUniqueId ?: fileUniqueId,
        )

    private fun List<Media>.isVisualAlbum(): Boolean =
        this.size >= 2 &&
            this.all {
                it.type == MediaType.IMAGE || it.type == MediaType.VIDEO
            }

    private fun List<Media>.allHaveFileId(): Boolean = all { it.lastFileId != null }

    private fun List<Media>.toMediaInputs(): List<MediaInput> =
        if (allHaveFileId()) {
            map { media -> MediaInput(type = media.type, file = media.lastFileId!!.asInputFile()) }
        } else {
            map { media -> MediaInput(type = media.type, file = File(media.fileUrl).asInputFile()) }
        }
}

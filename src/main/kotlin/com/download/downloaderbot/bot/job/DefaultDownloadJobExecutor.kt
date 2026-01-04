package com.download.downloaderbot.bot.job

import com.download.downloaderbot.app.config.properties.CacheProperties
import com.download.downloaderbot.app.download.MediaService
import com.download.downloaderbot.bot.commands.util.InputValidator
import com.download.downloaderbot.bot.config.properties.BotIdentity
import com.download.downloaderbot.bot.config.properties.BotProperties
import com.download.downloaderbot.bot.gateway.BotPort
import com.download.downloaderbot.bot.gateway.GatewayResult
import com.download.downloaderbot.bot.gateway.InputFile
import com.download.downloaderbot.bot.gateway.MessageOptions
import com.download.downloaderbot.bot.gateway.asInputFile
import com.download.downloaderbot.bot.gateway.telegram.fileId
import com.download.downloaderbot.bot.gateway.telegram.fileUniqueId
import com.download.downloaderbot.bot.gateway.toInputFile
import com.download.downloaderbot.bot.promo.PromoService
import com.download.downloaderbot.bot.ui.shareKeyboard
import com.download.downloaderbot.core.cache.CachePort
import com.download.downloaderbot.core.domain.Media
import com.download.downloaderbot.core.domain.MediaType
import com.download.downloaderbot.core.downloader.MediaNotFoundException
import com.download.downloaderbot.infra.metrics.BotMetrics
import com.github.kotlintelegrambot.entities.Message
import io.micrometer.core.instrument.Timer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import org.springframework.stereotype.Component
import java.io.File
import java.time.Duration

private val log = KotlinLogging.logger {}

@Suppress("LongParameterList")
@Component
class DefaultDownloadJobExecutor(
    private val service: MediaService,
    private val botPort: BotPort,
    private val props: BotProperties,
    private val cachePort: CachePort<String, List<Media>>,
    private val cacheProps: CacheProperties,
    private val promoService: PromoService,
    private val botIdentity: BotIdentity,
    private val botMetrics: BotMetrics,
    private val validator: InputValidator
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
        botMetrics.jobQueueDelayTimer(JOB_NAME)
            .record(Duration.ofNanos(startNanos - job.enqueuedAtNanos))

        val executionSample = Timer.start()
        return try {
            block()
        } finally {
            executionSample.stop(botMetrics.jobDurationTimer(JOB_NAME))
            val totalNanos = System.nanoTime() - job.enqueuedAtNanos
            botMetrics.jobTotalDurationTimer(JOB_NAME)
                .record(Duration.ofNanos(totalNanos))
        }
    }

    private suspend fun sendMediaSmart(
        chatId: Long,
        mediaList: List<Media>,
        replyTo: Long?,
    ): List<Message> {
        val sendPromo = promoService.shouldSend(chatId)

        return if (mediaList.isImageAlbum()) {
            val inputs = mediaList.toInputFiles()
            val chunked = mediaList.size > TELEGRAM_ALBUM_LIMIT
            when (val res = sendAlbum(chatId, inputs, replyTo, chunked)) {
                is GatewayResult.Ok -> {
                    val sentPhotos = res.value
                    if (sendPromo) {
                        botPort.sendText(
                            chatId,
                            props.promoText,
                            replyToMessageId = sentPhotos.firstOrNull()?.messageId ?: replyTo,
                            replyMarkup = share,
                        )
                    }
                    sentPhotos
                }
                is GatewayResult.Err -> {
                    log.warn(res.cause) { res.description }
                    emptyList()
                }
            }
        } else {
            val results =
                mediaList.map { media ->
                    val input = media.toInputFile()
                    val options =
                        MessageOptions(
                            caption = if (sendPromo) props.promoText else null,
                            replyToMessageId = replyTo,
                            replyMarkup = if (sendPromo) share else null,
                        )
                    botPort.sendMedia(
                        media.type,
                        chatId,
                        input,
                        options = options,
                    )
                }

            results.mapNotNull { result ->
                result.onErr { log.warn(it.cause) { it.description } }
                    .getOrNull()
            }
        }
    }

    private suspend fun sendAlbum(
        chatId: Long,
        inputs: List<InputFile>,
        replyTo: Long?,
        chunked: Boolean,
    ): GatewayResult<List<Message>> =
        if (chunked) {
            botPort.sendPhotoAlbumChunked(
                chatId,
                inputs,
                TELEGRAM_ALBUM_LIMIT,
                replyToMessageId = replyTo,
            )
        } else {
            botPort.sendPhotoAlbum(chatId, inputs, replyToMessageId = replyTo)
        }

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
            mediaList.zip(messages)
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

    private fun List<Media>.isImageAlbum(): Boolean = this.size >= 2 && this.first().type == MediaType.IMAGE

    private fun List<Media>.allHaveFileId(): Boolean = all { it.lastFileId != null }

    private fun List<Media>.toInputFiles(): List<InputFile> =
        if (allHaveFileId()) {
            map { it.lastFileId!!.asInputFile() }
        } else {
            map { File(it.fileUrl).asInputFile() }
        }
}

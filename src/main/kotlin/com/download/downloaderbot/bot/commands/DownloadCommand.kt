package com.download.downloaderbot.bot.commands

import com.download.downloaderbot.app.config.properties.CacheProperties
import com.download.downloaderbot.app.download.MediaService
import com.download.downloaderbot.bot.commands.util.UrlValidator
import com.download.downloaderbot.bot.config.properties.BotProperties
import com.download.downloaderbot.bot.gateway.BotPort
import com.download.downloaderbot.bot.gateway.GatewayResult
import com.download.downloaderbot.bot.gateway.InputFile
import com.download.downloaderbot.bot.gateway.asInputFile
import com.download.downloaderbot.bot.gateway.telegram.chatId
import com.download.downloaderbot.bot.gateway.telegram.fileId
import com.download.downloaderbot.bot.gateway.telegram.fileUniqueId
import com.download.downloaderbot.bot.gateway.telegram.isGroupChat
import com.download.downloaderbot.bot.gateway.telegram.isPrivateChat
import com.download.downloaderbot.bot.gateway.telegram.replyToMessageId
import com.download.downloaderbot.bot.gateway.toInputFile
import com.download.downloaderbot.bot.promo.PromoService
import com.download.downloaderbot.bot.ratelimit.guard.RateLimitGuard
import com.download.downloaderbot.bot.ui.shareKeyboard
import com.download.downloaderbot.core.cache.CachePort
import com.download.downloaderbot.core.domain.Media
import com.download.downloaderbot.core.domain.MediaType
import com.download.downloaderbot.core.downloader.MediaNotFoundException
import com.github.kotlintelegrambot.entities.Message
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import org.springframework.stereotype.Component
import java.io.File

private val log = KotlinLogging.logger {}

@Suppress("LongParameterList")
@Component
class DownloadCommand(
    private val service: MediaService,
    private val botPort: BotPort,
    private val props: BotProperties,
    private val rateLimitGuard: RateLimitGuard,
    private val validator: UrlValidator,
    private val cachePort: CachePort<String, List<Media>>,
    private val cacheProps: CacheProperties,
    private val promoService: PromoService,
) : BotCommand {
    private companion object {
        const val TELEGRAM_ALBUM_LIMIT = 10
    }

    override val name: String = "download"

    private val share by lazy { shareKeyboard(props.username, props.shareText) }

    override suspend fun handle(ctx: CommandContext) {
        val replyTo = ctx.replyToMessageId
        val url = ctx.args.firstOrNull()?.trim() ?: ""
        val isNotUrl = !validator.isHttpUrl(url)

        val allowed =
            when {
                isNotUrl -> false
                ctx.isPrivateChat -> true
                ctx.isGroupChat -> service.supports(url)
                else -> false
            }

        if (!allowed) {
            if (ctx.isPrivateChat) {
                log.info { "Executing /$name command but not URL provided" }
                rateLimitGuard.runOrReject(ctx) {
                    botPort.sendText(ctx.chatId, "Будь ласка, вкажіть URL для завантаження.", replyTo)
                }
            }
            return
        }

        log.info { "Executing /$name command with url: $url" }
        val mediaList =
            withContext(Dispatchers.IO) {
                rateLimitGuard.runOrReject(ctx) {
                    service.download(url)
                }
            }

        val normalizedUrl = mediaList.first().sourceUrl
        if (mediaList.isEmpty()) {
            throw MediaNotFoundException(normalizedUrl)
        }
        val messages = sendMediaSmart(ctx, mediaList, replyTo)
        updateCache(normalizedUrl, mediaList, messages)

        log.info {
            val titles = mediaList.map { it.title }
            val paths = mediaList.map { it.fileUrl }
            "Downloaded: $titles ($paths)"
        }
    }

    private suspend fun sendMediaSmart(
        ctx: CommandContext,
        mediaList: List<Media>,
        replyTo: Long?,
    ): List<Message> {
        val sendPromo = promoService.shouldSend(ctx.chatId)
        return if (mediaList.isImageAlbum()) {
            val inputs = mediaList.toInputFiles()
            val chunked = mediaList.size > TELEGRAM_ALBUM_LIMIT
            when (val res = sendAlbum(ctx, inputs, replyTo, chunked)) {
                is GatewayResult.Ok -> {
                    val sentPhotos = res.value
                    if (sendPromo) {
                        botPort.sendText(
                            ctx.chatId,
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
                    botPort.sendMedia(
                        media.type,
                        ctx.chatId,
                        input,
                        caption = if (sendPromo) props.promoText else null,
                        replyToMessageId = replyTo,
                        replyMarkup = if (sendPromo) share else null,
                    )
                }

            // TODO: handle many videos properly
            results.mapNotNull { result ->
                result.onErr { log.warn(it.cause) { it.description } }
                    .getOrNull()
            }
        }
    }

    private suspend fun sendAlbum(
        ctx: CommandContext,
        inputs: List<InputFile>,
        replyTo: Long?,
        chunked: Boolean,
    ): GatewayResult<List<Message>> =
        if (chunked) {
            botPort.sendPhotoAlbumChunked(
                ctx.chatId,
                inputs,
                TELEGRAM_ALBUM_LIMIT,
                replyToMessageId = replyTo,
            )
        } else {
            botPort.sendPhotoAlbum(ctx.chatId, inputs, replyToMessageId = replyTo)
        }

    private suspend fun updateCache(
        url: String,
        mediaList: List<Media>,
        messages: List<Message>,
    ) {
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

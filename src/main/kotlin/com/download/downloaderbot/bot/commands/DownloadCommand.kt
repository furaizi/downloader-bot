package com.download.downloaderbot.bot.commands

import com.download.downloaderbot.bot.gateway.BotPort
import com.download.downloaderbot.bot.gateway.telegram.chatId
import com.download.downloaderbot.bot.gateway.telegram.isGroupChat
import com.download.downloaderbot.bot.gateway.telegram.isPrivateChat
import com.download.downloaderbot.bot.gateway.telegram.replyToMessageId
import com.download.downloaderbot.bot.ratelimit.guard.RateLimitGuard
import com.download.downloaderbot.core.domain.Media
import com.download.downloaderbot.core.domain.MediaType
import com.download.downloaderbot.core.downloader.MediaNotFoundException
import com.download.downloaderbot.app.download.MediaService
import com.download.downloaderbot.app.download.UrlNormalizer
import com.download.downloaderbot.bot.gateway.GatewayResult
import com.download.downloaderbot.bot.gateway.InputFile
import com.download.downloaderbot.bot.gateway.asInputFile
import com.download.downloaderbot.bot.gateway.telegram.fileId
import com.download.downloaderbot.bot.gateway.telegram.fileUniqueId
import com.download.downloaderbot.bot.gateway.toInputFile
import com.download.downloaderbot.core.cache.CachePort
import com.download.downloaderbot.core.net.FinalUrlResolver
import com.download.downloaderbot.core.security.UrlAllowlist
import com.github.kotlintelegrambot.entities.Message
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import org.springframework.stereotype.Component
import java.io.File
import java.net.URI

private val log = KotlinLogging.logger {}

@Component
class DownloadCommand(
    private val service: MediaService,
    private val botPort: BotPort,
    private val allowlist: UrlAllowlist,
    private val rateLimitGuard: RateLimitGuard,
    private val urlResolver: FinalUrlResolver,
    private val urlNormalizer: UrlNormalizer,
    private val cachePort: CachePort<String, List<Media>>
) : BotCommand {

    private companion object {
        const val TELEGRAM_ALBUM_LIMIT = 10
    }

    override val name: String = "download"

    override suspend fun handle(ctx: CommandContext) {
        val replyTo = ctx.replyToMessageId
        val rawUrl = ctx.args.firstOrNull()?.trim()

        val isUrl = !rawUrl.isNullOrBlank() && looksLikeHttpUrl(rawUrl)
        val url = if (isUrl)
            urlNormalizer.normalize(urlResolver.resolve(rawUrl!!))
            else null

        val isValid = when {
            ctx.isPrivateChat -> isUrl
            ctx.isGroupChat -> isUrl && allowlist.isAllowed(url!!)
            else -> false
        }
        url!!

        if (!isValid) {
            if (ctx.isPrivateChat)
                rateLimitGuard.runOrReject(ctx) {
                    botPort.sendText(ctx.chatId, "Будь ласка, вкажіть URL для завантаження.", replyTo)
                }
            return
        }

        val mediaList = withContext(Dispatchers.IO) {
            rateLimitGuard.runOrReject(ctx) {
                service.download(url)
            }
        }
        if (mediaList.isEmpty())
            throw MediaNotFoundException(url)

        when {
            mediaList.isImageAlbum() && mediaList.size <= TELEGRAM_ALBUM_LIMIT ->
                botPort.sendPhotoAlbum(ctx.chatId, mediaList.toInputFiles(), replyToMessageId = replyTo)
                    .onOk { messages ->
                        val updated = mediaList.zip(messages)
                            .map { (media, message) -> media.updateWith(message) }
                        cachePort.put(url, updated)
                    }

            mediaList.isImageAlbum() && mediaList.size > TELEGRAM_ALBUM_LIMIT ->
                botPort.sendPhotoAlbumChunked(ctx.chatId, mediaList.toInputFiles(), replyToMessageId = replyTo)
                    .onOk { messages ->
                        val updated = mediaList.zip(messages)
                            .map { (media, message) -> media.updateWith(message) }
                        cachePort.put(url, updated)
                }

            else -> {
                val results = mediaList.map { media ->
                    val input = media.toInputFile()
                    botPort.sendMedia(media.type, ctx.chatId, input, replyToMessageId = replyTo)
                }
                val updated = mediaList.zip(results)
                    .map { (media, result) ->
                        val message = result.getOrNull()
                        if (message != null)
                            media.updateWith(message)
                        else media
                    }

                if (updated.any { it.lastFileId != null }) {
                    cachePort.put(url, updated)
                }
            }
        }



        val titles = mediaList.map { it.title }
        val urls = mediaList.map { it.fileUrl }
        log.info { "Downloaded: $titles ($urls)" }
    }

    private fun looksLikeHttpUrl(s: String): Boolean =
        try {
            val u = URI(s)
            (u.scheme == "http" || u.scheme == "https") && !u.host.isNullOrBlank()
        } catch (_: Exception) {
            false
        }

    private fun Media.updateWith(message: Message): Media =
        copy(
            lastFileId = message.fileId ?: lastFileId,
            fileUniqueId = message.fileUniqueId ?: fileUniqueId
        )

    private fun List<Media>.isImageAlbum(): Boolean =
        this.size >= 2 && this.first().type == MediaType.IMAGE

    private fun List<Media>.allHaveFileId(): Boolean = all { it.lastFileId != null }

    private fun List<Media>.toInputFiles(): List<InputFile> = if (allHaveFileId())
        map { it.lastFileId!!.asInputFile() }
    else
        map { File(it.fileUrl).asInputFile() }

}

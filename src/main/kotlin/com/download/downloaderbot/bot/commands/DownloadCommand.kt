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
import com.download.downloaderbot.bot.gateway.telegram.fileId
import com.download.downloaderbot.bot.gateway.telegram.fileUniqueId
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
            isImageAlbum(mediaList) && mediaList.size <= TELEGRAM_ALBUM_LIMIT ->
                sendImagesAsAlbum(ctx, mediaList, replyTo).onOk { messages ->
                    val updated = mediaList.zip(messages)
                        .map { (media, message) -> updatedWithMsg(media, message) }
                    cachePort.put(url, updated)
                }

            isImageAlbum(mediaList) && mediaList.size > TELEGRAM_ALBUM_LIMIT ->
                sendImagesAsAlbumChunked(ctx, mediaList, replyTo).onOk { messages ->
                    val updated = mediaList.zip(messages)
                        .map { (media, message) -> updatedWithMsg(media, message) }
                    cachePort.put(url, updated)
                }

            else -> {
                val results = sendIndividually(ctx, mediaList, replyTo)
                val updated = mediaList.zip(results).map { (m, r) ->
                    val msg = r.getOrNull()
                    if (msg != null)
                        updatedWithMsg(m, msg)
                    else m
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

    private fun isImageAlbum(mediaList: List<Media>) =
        mediaList.first().type == MediaType.IMAGE && mediaList.size >= 2

    private suspend fun sendImagesAsAlbum(
        ctx: CommandContext,
        mediaList: List<Media>,
        replyTo: Long?
    ): GatewayResult<List<Message>> {
        val allHaveFileId = mediaList.all { it.lastFileId != null }
        return if (allHaveFileId) {
            val fileIds = mediaList.map { it.lastFileId!! }
            botPort.sendPhotoAlbumIds(ctx.chatId, fileIds, replyToMessageId = replyTo)
        } else {
            val files = mediaList.map { File(it.fileUrl) }
            botPort.sendPhotoAlbumFiles(ctx.chatId, files, replyToMessageId = replyTo)
        }
    }

    private suspend fun sendImagesAsAlbumChunked(
        ctx: CommandContext,
        mediaList: List<Media>,
        replyTo: Long?
    ): GatewayResult<List<Message>> {
        val allHaveFileId = mediaList.all { it.lastFileId != null }
        return if (allHaveFileId) {
            val fileIds = mediaList.map { it.lastFileId!! }
            botPort.sendPhotoAlbumChunkedIds(ctx.chatId, fileIds, replyToMessageId = replyTo)
        } else {
            val files = mediaList.map { File(it.fileUrl) }
            botPort.sendPhotoAlbumChunkedFiles(ctx.chatId, files, replyToMessageId = replyTo)
        }
    }

    private suspend fun sendIndividually(
        ctx: CommandContext,
        mediaList: List<Media>,
        replyTo: Long?
    ): List<GatewayResult<Message>> =
        mediaList.map { media ->
            if (media.lastFileId != null) {
                sendByFileId(media.type, ctx.chatId, media.lastFileId, replyTo)
            } else {
                val file = File(media.fileUrl)
                sendByFile(media.type, ctx.chatId, file, replyTo)
            }
        }


    private suspend fun sendByFileId(
        type: MediaType,
        chatId: Long,
        fileId: String,
        replyTo: Long?
    ): GatewayResult<Message> = when (type) {
        MediaType.VIDEO -> botPort.sendVideo(chatId, fileId, replyToMessageId = replyTo)
        MediaType.AUDIO -> botPort.sendAudio(chatId, fileId, replyToMessageId = replyTo)
        MediaType.IMAGE -> botPort.sendPhoto(chatId, fileId, replyToMessageId = replyTo)
    }

    private suspend fun sendByFile(
        type: MediaType,
        chatId: Long,
        file: File,
        replyTo: Long?
    ): GatewayResult<Message> = when (type) {
        MediaType.VIDEO -> botPort.sendVideo(chatId, file, replyToMessageId = replyTo)
        MediaType.AUDIO -> botPort.sendAudio(chatId, file, replyToMessageId = replyTo)
        MediaType.IMAGE -> botPort.sendPhoto(chatId, file, replyToMessageId = replyTo)
    }

    private fun updatedWithMsg(media: Media, message: Message): Media =
        media.copy(
            lastFileId = message.fileId ?: media.lastFileId,
            fileUniqueId = message.fileUniqueId ?: media.fileUniqueId
        )

}

package com.download.downloaderbot.bot.commands

import com.download.downloaderbot.bot.gateway.TelegramGateway
import com.download.downloaderbot.bot.gateway.chatId
import com.download.downloaderbot.bot.gateway.isGroupChat
import com.download.downloaderbot.bot.gateway.isPrivateChat
import com.download.downloaderbot.bot.gateway.replyToMessageId
import com.download.downloaderbot.core.domain.Media
import com.download.downloaderbot.core.domain.MediaType
import com.download.downloaderbot.core.downloader.MediaNotFoundException
import com.download.downloaderbot.core.service.MediaDownloadService
import com.download.downloaderbot.core.service.net.UrlResolver
import com.download.downloaderbot.core.service.security.UrlAllowlist
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import org.springframework.stereotype.Component
import java.io.File
import java.net.URI

private val log = KotlinLogging.logger {}

@Component
class DownloadCommand(
    private val mediaDownloadService: MediaDownloadService,
    private val gateway: TelegramGateway,
    private val allowlist: UrlAllowlist,
    private val urlResolver: UrlResolver
) : BotCommand {

    private companion object {
        const val TELEGRAM_ALBUM_LIMIT = 10
    }

    override val name: String = "download"

    override suspend fun handle(ctx: CommandContext) {
        val replyTo = ctx.replyToMessageId
        val rawUrl = ctx.args.firstOrNull()?.trim()

        val isValid = when {
            ctx.isPrivateChat ->
                !rawUrl.isNullOrBlank() && looksLikeHttpUrl(rawUrl)
            ctx.isGroupChat ->
                !rawUrl.isNullOrBlank() && allowlist.isAllowed(urlResolver.finalUrl(rawUrl))
            else -> false
        }

        if (!isValid) {
            if (ctx.isPrivateChat)
                gateway.replyText(ctx.chatId, "Будь ласка, вкажіть URL для завантаження.", replyTo)
            return
        }

        val url = rawUrl!!
        val mediaList = withContext(Dispatchers.IO) {
            mediaDownloadService.download(url)
        }
        if (mediaList.isEmpty())
            throw MediaNotFoundException(url)

        when {
            isImageAlbum(mediaList) && mediaList.size <= TELEGRAM_ALBUM_LIMIT ->
                sendImagesAsAlbum(ctx, mediaList, replyTo)

            isImageAlbum(mediaList) && mediaList.size > TELEGRAM_ALBUM_LIMIT ->
                sendImagesAsAlbumChunked(ctx, mediaList, replyTo)

            else -> sendIndividually(ctx, mediaList, replyTo)
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
    ) {
        val files = mediaList.map { File(it.fileUrl) }
        gateway.sendPhotosAlbum(ctx.chatId, files, replyToMessageId = replyTo)
    }

    private suspend fun sendImagesAsAlbumChunked(
        ctx: CommandContext,
        mediaList: List<Media>,
        replyTo: Long?
    ) {
        val files = mediaList.map { File(it.fileUrl) }
        gateway.sendPhotosAlbumChunked(ctx.chatId, files, replyToMessageId = replyTo)
    }

    private suspend fun sendIndividually(
        ctx: CommandContext,
        mediaList: List<Media>,
        replyTo: Long?
    ) {
        mediaList.forEach { media ->
            val file = File(media.fileUrl)
            when (media.type) {
                MediaType.VIDEO -> gateway.sendVideo(ctx.chatId, file, replyToMessageId = replyTo)
                MediaType.AUDIO -> gateway.sendAudio(ctx.chatId, file, replyToMessageId = replyTo)
                MediaType.IMAGE -> gateway.sendPhoto(ctx.chatId, file, replyToMessageId = replyTo)
            }
        }
    }

}

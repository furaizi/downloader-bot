package com.download.downloaderbot.bot.handler

import com.download.downloaderbot.bot.commands.CommandContext
import com.download.downloaderbot.bot.commands.TelegramGateway
import com.download.downloaderbot.bot.commands.chatId
import com.download.downloaderbot.core.downloader.MediaDownloaderException
import com.download.downloaderbot.core.downloader.MediaDownloaderToolException
import com.download.downloaderbot.core.downloader.MediaNotFoundException
import com.download.downloaderbot.core.downloader.MediaTooLargeException
import com.download.downloaderbot.core.downloader.ToolExecutionException
import com.download.downloaderbot.core.downloader.UnsupportedSourceException
import com.download.downloaderbot.core.downloader.toMB
import mu.KotlinLogging
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

@Component
class GlobalTelegramExceptionHandler(val gateway: TelegramGateway) {

    suspend fun handle(e: Exception, ctx: CommandContext) {
        logAtProperLevel(e)
        gateway.replyText(ctx.chatId, e.toUserMessage())
    }


    private fun Exception.toUserMessage(): String = when (this) {
        is UnsupportedSourceException -> "This source is not supported."
        is MediaTooLargeException -> "Media is too large. Limit: ${limit.toMB()} MB"
        is MediaNotFoundException -> "No media found at the provided URL."
        is ToolExecutionException -> "An internal tool failed to execute (code=$exitCode)."
        is MediaDownloaderToolException -> "An internal tool error occurred."
        is MediaDownloaderException -> "An error occurred while processing the media."
        else -> "An unexpected error occurred."
    }

    private fun logAtProperLevel(e: Exception) = when (e) {
        is UnsupportedSourceException -> { log.info { e.message } }
        is MediaTooLargeException -> { log.info { e.message } }
        is MediaNotFoundException -> { log.info { e.message } }
        is ToolExecutionException -> { log.warn { e.message } }
        is MediaDownloaderToolException -> { log.warn { e.message } }
        is MediaDownloaderException -> { log.warn { e.message } }
        else -> { log.error(e) { e.message ?: "No message" } }
    }
}
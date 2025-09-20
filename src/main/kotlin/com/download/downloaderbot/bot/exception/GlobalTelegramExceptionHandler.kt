package com.download.downloaderbot.bot.exception

import com.download.downloaderbot.bot.commands.CommandContext
import com.download.downloaderbot.bot.gateway.TelegramGateway
import com.download.downloaderbot.bot.gateway.chatId
import com.download.downloaderbot.bot.gateway.replyToMessageId
import com.download.downloaderbot.core.downloader.BusyException
import com.download.downloaderbot.core.downloader.DownloadInProgressException
import com.download.downloaderbot.core.downloader.MediaDownloaderException
import com.download.downloaderbot.core.downloader.MediaDownloaderToolException
import com.download.downloaderbot.core.downloader.MediaNotFoundException
import com.download.downloaderbot.core.downloader.MediaTooLargeException
import com.download.downloaderbot.core.downloader.ToolExecutionException
import com.download.downloaderbot.core.downloader.ToolTimeoutException
import com.download.downloaderbot.core.downloader.UnsupportedSourceException
import com.download.downloaderbot.core.downloader.toMB
import kotlinx.coroutines.CancellationException
import mu.KotlinLogging
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

@Component
class GlobalTelegramExceptionHandler(val gateway: TelegramGateway) {

    suspend fun handle(e: Exception, ctx: CommandContext) {
        if (e is CancellationException)
            throw e // very important for coroutines
        logAtProperLevel(e, ctx.chatId)
        gateway.replyText(ctx.chatId, e.toUserMessage(), ctx.replyToMessageId)
    }


    private fun Exception.toUserMessage(): String = when (this) {
        is UnsupportedSourceException ->    "Це джерело не підтримується."
        is MediaTooLargeException ->        "Медіафайл занадто великий. Обмеження: ${limit.toMB()} МБ."
        is MediaNotFoundException ->        "Нічого не знайдено за цим URL."
        is ToolExecutionException ->        "Внутрішній інструмент не зміг виконатися (код=$exitCode)."
        is ToolTimeoutException ->          "Внутрішній інструмент перевищив час очікування: $timeout."
        is MediaDownloaderToolException ->  "Сталася внутрішня помилка інструменту."
        is DownloadInProgressException ->   "Цей медіафайл уже завантажується, будь ласка, зачекайте."
        is BusyException ->                 "Завантажувач зараз зайнятий, спробуйте пізніше."
        is MediaDownloaderException ->      "Сталася помилка під час обробки медіафайлу."
        else ->                             "Сталася непередбачена помилка."
    }

    private fun logAtProperLevel(e: Exception, chatId: Long) {
        val msg = e.message ?: e::class.simpleName ?: "No message"
        val base = "[chatId=$chatId] $msg"

        when (e) {
            is UnsupportedSourceException,
            is MediaTooLargeException,
            is MediaNotFoundException,
            is DownloadInProgressException,
            is BusyException -> {
                log.info { base }
                log.debug(e) { base }
            }
            is ToolTimeoutException,
            is ToolExecutionException,
            is MediaDownloaderToolException,
            is MediaDownloaderException -> log.warn(e) { base }
            else -> log.error(e) { base }
        }
    }
}

package com.download.downloaderbot.bot.exception

import com.download.downloaderbot.bot.commands.CommandContext
import com.download.downloaderbot.bot.gateway.telegram.chatId
import com.download.downloaderbot.bot.gateway.telegram.replyToMessageId
import com.download.downloaderbot.core.downloader.*
import com.download.downloaderbot.infra.metrics.BotMetrics
import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.ChatId
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CancellationException
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

@Component
class GlobalTelegramExceptionHandler(
    val bot: Bot,
    private val botMetrics: BotMetrics,
) {
    suspend fun handle(
        e: Exception,
        ctx: CommandContext,
    ) {
        if (e is CancellationException) {
            throw e // very important for coroutines
        }
        botMetrics.errors.increment()
        logAtProperLevel(e, ctx.chatId)
        bot.sendMessage(
            chatId = ChatId.fromId(ctx.chatId),
            text = e.toUserMessage(),
            replyToMessageId = ctx.replyToMessageId
        )
    }

    private fun Exception.toUserMessage(): String =
        when (this) {
            is UnsupportedSourceException -> "Це джерело не підтримується."
            is MediaTooLargeException -> "Медіафайл занадто великий. Обмеження: ${limit.toMB()} МБ."
            is MediaNotFoundException -> "Нічого не знайдено за цим URL."
            is ToolExecutionException -> "Внутрішній інструмент не зміг виконатися."
            is ToolTimeoutException -> "Внутрішній інструмент перевищив час очікування: $timeout."
            is MediaDownloaderToolException -> "Сталася внутрішня помилка інструменту."
            is DownloadInProgressException -> "Цей медіафайл уже завантажується, будь ласка, зачекайте."
            is BusyException -> "Завантажувач зараз зайнятий, спробуйте пізніше."
            is TooManyRequestsException -> "Занадто багато запитів, спробуйте пізніше."
            is MediaDownloaderException -> "Сталася помилка під час обробки медіафайлу."
            else -> "Сталася непередбачена помилка."
        }

    private fun logAtProperLevel(
        e: Exception,
        chatId: Long,
    ) {
        val msg = e.message ?: e::class.simpleName ?: "No message"
        val base = "[chatId=$chatId] $msg"

        when (e) {
            is UnsupportedSourceException,
            is MediaTooLargeException,
            is MediaNotFoundException,
            is DownloadInProgressException,
            is BusyException,
            is TooManyRequestsException,
            -> {
                log.info { base }
                log.debug(e) { base }
            }
            is ToolTimeoutException,
            is ToolExecutionException,
            is MediaDownloaderToolException,
            is MediaDownloaderException,
            -> log.warn(e) { base }
            else -> log.error(e) { base }
        }
    }
}

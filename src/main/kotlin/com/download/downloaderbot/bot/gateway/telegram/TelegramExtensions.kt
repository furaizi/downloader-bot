package com.download.downloaderbot.bot.gateway.telegram

import com.download.downloaderbot.bot.exception.*
import com.github.kotlintelegrambot.types.TelegramBotResult

fun <T : Any> TelegramBotResult<T>.toResult(): Result<T> =
    when (this) {
        is TelegramBotResult.Success -> Result.success(this.value)
        is TelegramBotResult.Error.HttpError -> Result.failure(
            TelegramHttpException(this.httpCode, this.description)
        )
        is TelegramBotResult.Error.TelegramApi -> Result.failure(
            TelegramApiException(this.errorCode, this.description)
        )
        is TelegramBotResult.Error.InvalidResponse -> Result.failure(
            TelegramInvalidResponseException(
                httpCode = this.httpCode,
                httpStatusMessage = this.httpStatusMessage,
                telegramErrorCode = this.body?.errorCode,
                telegramErrorDescription = this.body?.errorDescription
            )
        )
        is TelegramBotResult.Error.Unknown -> Result.failure(
            TelegramUnknownException(this.exception)
        )
    }

fun <T : Any> TelegramBotResult<T>.getOrThrow(): T = toResult().getOrThrow()

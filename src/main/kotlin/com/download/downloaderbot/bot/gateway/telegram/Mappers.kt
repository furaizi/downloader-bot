package com.download.downloaderbot.bot.gateway.telegram

import com.download.downloaderbot.bot.gateway.GatewayResult
import com.download.downloaderbot.bot.gateway.InputFile
import com.github.kotlintelegrambot.entities.TelegramFile
import com.github.kotlintelegrambot.types.TelegramBotResult
import com.github.kotlintelegrambot.network.Response as TgEnvelope
import retrofit2.Response as HttpResponse

fun InputFile.toTelegram(): TelegramFile =
    when (this) {
        is InputFile.Local -> TelegramFile.ByFile(this.file)
        is InputFile.Id -> TelegramFile.ByFileId(this.fileId)
    }

fun <T : Any> TelegramBotResult<T>.toGateway(): GatewayResult<T> =
    when (this) {
        is TelegramBotResult.Success -> GatewayResult.Ok(this.value)
        is TelegramBotResult.Error.HttpError ->
            GatewayResult.Err(
                kind = GatewayResult.Err.Kind.HTTP,
                httpCode = this.httpCode,
                description = this.description,
            )
        is TelegramBotResult.Error.TelegramApi ->
            GatewayResult.Err(
                kind = GatewayResult.Err.Kind.TELEGRAM,
                telegramCode = this.errorCode,
                description = this.description,
            )
        is TelegramBotResult.Error.InvalidResponse ->
            GatewayResult.Err(
                kind = GatewayResult.Err.Kind.INVALID_RESPONSE,
                httpCode = this.httpCode,
                description = "Invalid Telegram response: ${this.body}",
            )
        is TelegramBotResult.Error.Unknown ->
            GatewayResult.Err(
                kind = GatewayResult.Err.Kind.EXCEPTION,
                cause = this.exception,
                description = this.exception.message,
            )
    }

fun <T : Any> Pair<HttpResponse<TgEnvelope<T>?>?, Exception?>.toGateway(): GatewayResult<T> {
    val (http, ex) = this
    if (ex != null) {
        return GatewayResult.Err(
            kind = GatewayResult.Err.Kind.EXCEPTION,
            cause = ex,
            description = ex.message,
        )
    }
    if (http == null) {
        return GatewayResult.Err(
            kind = GatewayResult.Err.Kind.UNKNOWN,
            description = "HTTP response is null",
        )
    }

    val code = http.code()
    if (!http.isSuccessful) {
        val bodyText =
            try {
                http.errorBody()?.string()
            } catch (_: Throwable) {
                null
            }
        return GatewayResult.Err(
            kind = GatewayResult.Err.Kind.HTTP,
            httpCode = code,
            description = bodyText ?: http.message(),
        )
    }

    val envelope =
        http.body()
            ?: return GatewayResult.Err(
                kind = GatewayResult.Err.Kind.INVALID_RESPONSE,
                httpCode = code,
                description = "Empty body",
            )

    if (!envelope.ok) {
        return GatewayResult.Err(
            kind = GatewayResult.Err.Kind.TELEGRAM,
            telegramCode = envelope.errorCode,
            description = envelope.errorDescription,
        )
    }

    val payload =
        envelope.result
            ?: return GatewayResult.Err(
                kind = GatewayResult.Err.Kind.INVALID_RESPONSE,
                httpCode = code,
                description = "Result is null",
            )

    return GatewayResult.Ok(payload)
}

package com.download.downloaderbot.bot.exception

sealed class TelegramBotException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

class TelegramApiException(
    val errorCode: Int,
    val description: String
) : TelegramBotException("Telegram API $errorCode: $description")

class TelegramHttpException(
    val httpCode: Int,
    val description: String?
) : TelegramBotException("HTTP $httpCode: ${description ?: "No description"}")

class TelegramInvalidResponseException(
    val httpCode: Int,
    val httpStatusMessage: String?,
    val telegramErrorCode: Int?,
    val telegramErrorDescription: String?
) : TelegramBotException(
    "Invalid response HTTP $httpCode (${httpStatusMessage ?: "Unknown"}). " +
        "Telegram details: code=${telegramErrorCode ?: "N/A"}, description=${telegramErrorDescription ?: "N/A"}"
)

class TelegramUnknownException(
    cause: Throwable
) : TelegramBotException("Unknown Telegram error: ${cause.message}", cause)

package com.download.downloaderbot.bot.gateway.telegram

import com.download.downloaderbot.bot.exception.TelegramApiException
import com.download.downloaderbot.bot.exception.TelegramHttpException
import com.download.downloaderbot.bot.exception.TelegramInvalidResponseException
import com.download.downloaderbot.bot.exception.TelegramUnknownException
import com.github.kotlintelegrambot.types.TelegramBotResult
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.result.shouldBeFailure
import io.kotest.matchers.result.shouldBeSuccess
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import com.github.kotlintelegrambot.network.Response as TgEnvelope
import retrofit2.Response as HttpResponse

class MappersTest :
    FunSpec({

        context("TelegramBotResult to Result") {
            test("maps success") {
                TelegramBotResult.Success("val").toResult().shouldBeSuccess("val")
            }

            test("maps HTTP error") {
                val failure = TelegramBotResult.Error.HttpError(404, "Not Found").toResult()
                val exception = failure.shouldBeFailure()

                exception.shouldBeTypeOf<TelegramHttpException>()
                exception.message shouldBe "HTTP 404: Not Found"
            }

            test("maps Telegram API error") {
                val failure = TelegramBotResult.Error.TelegramApi(400, "Bad Request").toResult()
                val exception = failure.shouldBeFailure()

                exception.shouldBeTypeOf<TelegramApiException>()
                exception.message shouldBe "Telegram API 400: Bad Request"
            }

            test("maps invalid response error") {
                val failure = TelegramBotResult.Error.InvalidResponse(500, "Err", null).toResult()
                val exception = failure.shouldBeFailure()

                exception.shouldBeTypeOf<TelegramInvalidResponseException>()
            }

            test("maps unknown error") {
                val cause = RuntimeException("Boom")
                val failure = TelegramBotResult.Error.Unknown(cause).toResult()
                val exception = failure.shouldBeFailure()

                exception.shouldBeTypeOf<TelegramUnknownException>()
                exception.cause shouldBe cause
            }
        }

        context("Retrofit pair to Result") {
            test("returns success when request and envelope are successful") {
                val pair: Pair<HttpResponse<TgEnvelope<String>?>?, Exception?> =
                    HttpResponse.success(TgEnvelope("Success data", true)) to null

                pair.toResult().shouldBeSuccess("Success data")
            }

            test("returns unknown error when exception is present") {
                val cause = RuntimeException("Net error")
                val pair: Pair<HttpResponse<TgEnvelope<String>?>?, Exception?> = null to cause
                val exception = pair.toResult().shouldBeFailure()

                exception.shouldBeTypeOf<TelegramUnknownException>()
                exception.cause shouldBe cause
            }

            test("returns unknown error when HTTP response is null") {
                val pair: Pair<HttpResponse<TgEnvelope<String>?>?, Exception?> = null to null
                val exception = pair.toResult().shouldBeFailure()

                exception.shouldBeTypeOf<TelegramUnknownException>()
                exception.message shouldBe "Unknown Telegram error: HTTP response is null"
            }

            test("returns HTTP error when response is not successful") {
                val pair: Pair<HttpResponse<TgEnvelope<String>?>?, Exception?> =
                    HttpResponse.error<TgEnvelope<String>?>(
                        404,
                        "Error body content".toResponseBody("text/plain".toMediaTypeOrNull()),
                    ) to null
                val exception = pair.toResult().shouldBeFailure()

                exception.shouldBeTypeOf<TelegramHttpException>()
                exception.message shouldBe "HTTP 404: Error body content"
            }

            test("returns invalid response error when body is null") {
                val pair: Pair<HttpResponse<TgEnvelope<String>?>?, Exception?> =
                    HttpResponse.success<TgEnvelope<String>?>(null) to null
                val exception = pair.toResult().shouldBeFailure()

                exception.shouldBeTypeOf<TelegramInvalidResponseException>()
            }

            test("returns Telegram API error when envelope is not ok") {
                val pair: Pair<HttpResponse<TgEnvelope<String>?>?, Exception?> =
                    HttpResponse.success(TgEnvelope<String>(null, false, 400, "Bad Request")) to null
                val exception = pair.toResult().shouldBeFailure()

                exception.shouldBeTypeOf<TelegramApiException>()
                exception.message shouldBe "Telegram API 400: Bad Request"
            }

            test("returns invalid response error when result is null but ok is true") {
                val pair: Pair<HttpResponse<TgEnvelope<String>?>?, Exception?> =
                    HttpResponse.success(TgEnvelope<String>(null, true)) to null
                val exception = pair.toResult().shouldBeFailure()

                exception.shouldBeTypeOf<TelegramInvalidResponseException>()
            }
        }
    })

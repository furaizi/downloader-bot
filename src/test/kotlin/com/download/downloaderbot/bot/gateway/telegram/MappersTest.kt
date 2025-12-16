package com.download.downloaderbot.bot.gateway.telegram

import com.download.downloaderbot.bot.gateway.GatewayResult
import com.download.downloaderbot.bot.gateway.InputFile
import com.github.kotlintelegrambot.entities.TelegramFile
import com.github.kotlintelegrambot.types.TelegramBotResult
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import java.io.File
import com.github.kotlintelegrambot.network.Response as TgEnvelope
import retrofit2.Response as HttpResponse

class MappersTest : FunSpec({

    context("InputFile mapping") {
        data class TestCase(val name: String, val input: InputFile, val expected: TelegramFile)

        listOf(
            TestCase(
                "maps Local to ByFile",
                InputFile.Local(File("test.txt")),
                TelegramFile.ByFile(File("test.txt")),
            ),
            TestCase(
                "maps Id to ByFileId",
                InputFile.Id("12345"),
                TelegramFile.ByFileId("12345"),
            ),
        ).forEach { (name, input, expected) ->
            test(name) {
                input.toTelegram() shouldBe expected
            }
        }
    }

    context("TelegramBotResult mapping") {
        data class TestCase(
            val name: String,
            val input: TelegramBotResult<String>,
            val expected: GatewayResult<String>,
        )

        val exception = RuntimeException("Boom")

        listOf(
            TestCase(
                "maps Success to Ok",
                TelegramBotResult.Success("val"),
                GatewayResult.Ok("val"),
            ),
            TestCase(
                "maps HttpError to Err(HTTP)",
                TelegramBotResult.Error.HttpError(404, "Not Found"),
                GatewayResult.Err(GatewayResult.Err.Kind.HTTP, 404, description = "Not Found"),
            ),
            TestCase(
                "maps TelegramApi to Err(TELEGRAM)",
                TelegramBotResult.Error.TelegramApi(400, "Bad Request"),
                GatewayResult.Err(GatewayResult.Err.Kind.TELEGRAM, telegramCode = 400, description = "Bad Request"),
            ),
            TestCase(
                "maps InvalidResponse to Err(INVALID_RESPONSE)",
                TelegramBotResult.Error.InvalidResponse(500, "Err", null),
                GatewayResult.Err(GatewayResult.Err.Kind.INVALID_RESPONSE, 500, description = "Invalid Telegram response: null"),
            ),
            TestCase(
                "maps Unknown to Err(EXCEPTION)",
                TelegramBotResult.Error.Unknown(exception),
                GatewayResult.Err(GatewayResult.Err.Kind.EXCEPTION, cause = exception, description = "Boom"),
            ),
        ).forEach { (name, input, expected) ->
            test(name) {
                input.toGateway() shouldBe expected
            }
        }
    }

    context("Retrofit Pair mapping") {
        data class TestCase(
            val name: String,
            val httpResponse: HttpResponse<TgEnvelope<String>?>?,
            val exception: Exception? = null,
            val expected: GatewayResult<String>,
        )

        val ex = RuntimeException("Net error")

        listOf(
            TestCase(
                "returns Err(EXCEPTION) when exception is present",
                null,
                ex,
                GatewayResult.Err(GatewayResult.Err.Kind.EXCEPTION, cause = ex, description = "Net error"),
            ),
            TestCase(
                "returns Err(UNKNOWN) when both http and exception are null",
                null,
                expected = GatewayResult.Err(GatewayResult.Err.Kind.UNKNOWN, description = "HTTP response is null"),
            ),
            TestCase(
                "returns Err(HTTP) when http response is not successful",
                HttpResponse.error(404, "Error body content".toResponseBody("text/plain".toMediaTypeOrNull())),
                expected = GatewayResult.Err(GatewayResult.Err.Kind.HTTP, 404, description = "Error body content"),
            ),
            TestCase(
                "returns Err(INVALID_RESPONSE) when body is null",
                HttpResponse.success<TgEnvelope<String>?>(null),
                expected = GatewayResult.Err(GatewayResult.Err.Kind.INVALID_RESPONSE, 200, description = "Empty body"),
            ),
            TestCase(
                "returns Err(TELEGRAM) when envelope is not ok",
                HttpResponse.success(TgEnvelope(null, false, 400, "Bad Request")),
                expected = GatewayResult.Err(GatewayResult.Err.Kind.TELEGRAM, telegramCode = 400, description = "Bad Request"),
            ),
            TestCase(
                "returns Err(INVALID_RESPONSE) when result is null but ok is true",
                HttpResponse.success(TgEnvelope(null, true)),
                expected = GatewayResult.Err(GatewayResult.Err.Kind.INVALID_RESPONSE, 200, description = "Result is null"),
            ),
            TestCase(
                "returns Ok when request successful and result present",
                HttpResponse.success(TgEnvelope("Success data", true)),
                expected = GatewayResult.Ok("Success data"),
            ),
        ).forEach { testCase ->
            test(testCase.name) {
                val pair: Pair<HttpResponse<TgEnvelope<String>?>?, Exception?> =
                    testCase.httpResponse to testCase.exception

                pair.toGateway() shouldBe testCase.expected
            }
        }
    }
})

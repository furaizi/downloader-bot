package com.download.downloaderbot.bot.gateway

sealed class GatewayResult<out T> {
    data class Ok<T>(val value: T) : GatewayResult<T>()

    data class Err(
        val kind: Kind,
        val httpCode: Int? = null,
        val telegramCode: Int? = null,
        val description: String? = null,
        val cause: Throwable? = null,
    ) : GatewayResult<Nothing>() {
        enum class Kind { HTTP, TELEGRAM, INVALID_RESPONSE, EXCEPTION, UNKNOWN }
    }

    suspend inline fun <R> fold(
        crossinline ifOk: suspend (T) -> R,
        crossinline ifErr: suspend (Err) -> R,
    ): R =
        when (this) {
            is Ok -> ifOk(value)
            is Err -> ifErr(this)
        }

    suspend inline fun onOk(crossinline block: suspend (T) -> Unit): GatewayResult<T> =
        also {
            if (this is Ok) {
                block(value)
            }
        }

    suspend inline fun onErr(crossinline block: suspend (Err) -> Unit): GatewayResult<T> =
        also {
            if (this is Err) {
                block(this)
            }
        }

    fun getOrNull(): T? = (this as? Ok)?.value
}

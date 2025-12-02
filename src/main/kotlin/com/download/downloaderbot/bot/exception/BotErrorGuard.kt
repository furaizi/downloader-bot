package com.download.downloaderbot.bot.exception

import com.download.downloaderbot.bot.commands.CommandContext
import org.springframework.stereotype.Component
import kotlin.coroutines.cancellation.CancellationException

@Component
class BotErrorGuard(
    private val exceptionHandler: GlobalTelegramExceptionHandler,
) {
    suspend fun <T> runSafely(
        ctx: CommandContext,
        block: suspend () -> T,
    ): T? =
        try {
            block()
        } catch (e: Throwable) {
            if (e is CancellationException) throw e

            val ex = e as? Exception ?: Exception(e)
            exceptionHandler.handle(ex, ctx)

            null
        }
}

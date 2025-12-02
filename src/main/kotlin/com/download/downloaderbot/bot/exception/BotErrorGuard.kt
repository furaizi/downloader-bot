package com.download.downloaderbot.bot.exception

import com.download.downloaderbot.bot.commands.CommandContext
import org.springframework.stereotype.Component
import kotlin.coroutines.cancellation.CancellationException

@Component
class BotErrorGuard(
    private val exceptionHandler: GlobalTelegramExceptionHandler,
) {
    @Suppress("TooGenericExceptionCaught")
    suspend fun <T> runSafely(
        ctx: CommandContext,
        block: suspend () -> T,
    ): T? =
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            exceptionHandler.handle(e, ctx)

            null
        }
}

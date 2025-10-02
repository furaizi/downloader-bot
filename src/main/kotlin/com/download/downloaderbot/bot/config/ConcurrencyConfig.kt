package com.download.downloaderbot.bot.config

import com.download.downloaderbot.bot.commands.CommandContext
import com.download.downloaderbot.bot.exception.GlobalTelegramExceptionHandler
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import mu.KotlinLogging
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.cancellation.CancellationException

private val log = KotlinLogging.logger {}

@Configuration
class ConcurrencyConfig(
    private val exceptionHandler: GlobalTelegramExceptionHandler,
) {
    data class BotContext(val commandContext: CommandContext) :
        AbstractCoroutineContextElement(Key) {
        companion object Key : CoroutineContext.Key<BotContext>
    }

    class BotCoroutineExceptionHandler(
        private val delegate: GlobalTelegramExceptionHandler,
    ) : AbstractCoroutineContextElement(CoroutineExceptionHandler), CoroutineExceptionHandler {
        private val notifyScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        override fun handleException(
            context: CoroutineContext,
            exception: Throwable,
        ) {
            val botCtx = context[BotContext] ?: return
            if (exception is CancellationException) return

            notifyScope.launch {
                try {
                    val castException = exception as? Exception ?: Exception(exception)
                    delegate.handle(castException, botCtx.commandContext)
                } catch (ce: CancellationException) {
                    throw ce
                } catch (e: Exception) {
                    log.error(e) { "Failed to notify user about exception: ${exception.message}" }
                }
            }
        }
    }

    @Bean
    fun botScope(): CoroutineScope {
        val handler = BotCoroutineExceptionHandler(exceptionHandler)
        return CoroutineScope(SupervisorJob() + Dispatchers.Default + handler)
    }

    @Bean
    fun maintenanceScope() = CoroutineScope(SupervisorJob() + Dispatchers.IO)
}

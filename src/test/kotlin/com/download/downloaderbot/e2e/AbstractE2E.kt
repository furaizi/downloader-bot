package com.download.downloaderbot.e2e

import com.download.downloaderbot.app.config.properties.MediaProperties
import com.download.downloaderbot.bot.commands.CommandContext
import com.download.downloaderbot.bot.core.UpdateHandler
import com.download.downloaderbot.bot.exception.BotErrorGuard
import com.download.downloaderbot.bot.gateway.RecordingBotPort
import com.github.kotlintelegrambot.entities.Update
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.SpringExtension
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import java.nio.file.Files
import java.nio.file.Path

abstract class AbstractE2E(
    protected val updateHandler: UpdateHandler,
    protected val botPort: RecordingBotPort,
    protected val mediaProps: MediaProperties,
    private val errorGuard: BotErrorGuard? = null,
    body: AbstractE2E.() -> Unit,
) : FunSpec({

    extension(SpringExtension)

    beforeTest {
        botPort.reset()
        mediaProps.basePath.toFile().deleteRecursively()
        Files.createDirectories(mediaProps.basePath)
    }
}) {

    init {
        body.invoke(this)
    }

    internal suspend fun handle(update: Update) {
        val guard = errorGuard
        if (guard == null) {
            updateHandler.handle(update)
            return
        }

        val args = update.message
            ?.text
            ?.trim()
            ?.split(Regex("\\s+"))
            .orEmpty()

        val ctx = CommandContext(update, args)

        guard.runSafely(ctx) {
            updateHandler.handle(update)
        }
    }

    companion object {
        private val mediaDir: Path = Files.createTempDirectory("downloader-bot-media-")

        init {
            Runtime.getRuntime().addShutdownHook(Thread {
                mediaDir.toFile().deleteRecursively()
            })
        }

        @JvmStatic
        @DynamicPropertySource
        fun props(registry: DynamicPropertyRegistry) {
            registry.add("downloader.media.base-dir") { mediaDir.toString() }
        }
    }
}

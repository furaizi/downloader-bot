package com.download.downloaderbot.bot.commands

import com.download.downloaderbot.bot.config.properties.BotProperties
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class CommandRegistryTest {
    @Test
    fun `registers commands by name`() {
        val startCmd = createCommand("start")
        val helpCmd = createCommand("help")
        val downloadCmd = createCommand("download")

        val registry =
            createRegistry(
                handlers = listOf(startCmd, helpCmd, downloadCmd),
                defaultCommand = "download",
            )

        assertEquals(startCmd, registry.byName["start"])
        assertEquals(helpCmd, registry.byName["help"])
        assertEquals(downloadCmd, registry.byName["download"])
    }

    @Test
    fun `sets default command correctly`() {
        val downloadCmd = createCommand("download")
        val helpCmd = createCommand("help")

        val registry =
            createRegistry(
                handlers = listOf(downloadCmd, helpCmd),
                defaultCommand = "download",
            )

        assertEquals(downloadCmd, registry.default)
    }

    @Test
    fun `throws error when default command not found`() {
        val helpCmd = createCommand("help")

        assertThrows<IllegalStateException> {
            createRegistry(
                handlers = listOf(helpCmd),
                defaultCommand = "nonexistent",
            )
        }
    }

    @Test
    fun `returns null for unknown command name`() {
        val registry =
            createRegistry(
                handlers = listOf(createCommand("download")),
                defaultCommand = "download",
            )

        assertNull(registry.byName["unknown"])
    }

    @Test
    fun `handles empty handlers list with existing default`() {
        assertThrows<IllegalStateException> {
            createRegistry(
                handlers = emptyList(),
                defaultCommand = "download",
            )
        }
    }

    private fun createCommand(name: String): BotCommand =
        mockk {
            every { this@mockk.name } returns name
            coEvery { handle(any()) } returns Unit
        }

    private fun createRegistry(
        handlers: List<BotCommand>,
        defaultCommand: String,
    ): CommandRegistry {
        val props =
            mockk<BotProperties> {
                every { this@mockk.defaultCommand } returns defaultCommand
            }
        return CommandRegistry(handlers, props)
    }
}

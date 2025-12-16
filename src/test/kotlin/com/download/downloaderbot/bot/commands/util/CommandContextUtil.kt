package com.download.downloaderbot.bot.commands.util

import com.download.downloaderbot.bot.commands.CommandContext
import com.github.kotlintelegrambot.entities.Chat
import com.github.kotlintelegrambot.entities.Message
import com.github.kotlintelegrambot.entities.Update
import io.mockk.every
import io.mockk.mockk

fun ctx(
    args: List<String> = emptyList(),
    chatId: Long = 123L,
    replyToMessageId: Long = 777L,
    chatType: String = if (chatId < 0) "group" else "private",
): CommandContext {
    val chat =
        mockk<Chat> {
            every { id } returns chatId
            every { type } returns chatType
        }

    val message =
        mockk<Message> {
            every { this@mockk.chat } returns chat
            every { messageId } returns replyToMessageId
        }

    val update =
        mockk<Update> {
            every { this@mockk.message } returns message
            every { callbackQuery } returns null
        }

    return CommandContext(update, args)
}

package com.download.downloaderbot.bot.commands.util

import com.download.downloaderbot.bot.commands.CommandContext
import com.github.kotlintelegrambot.entities.Chat
import com.github.kotlintelegrambot.entities.Message
import com.github.kotlintelegrambot.entities.Update
import io.mockk.every
import io.mockk.mockk

fun ctx(
    args: List<String>,
    chatId: Long,
    messageId: Long,
    chatType: String,
): CommandContext {
    val update = mockk<Update>()
    val message = mockk<Message>()
    val chat = mockk<Chat>()

    every { chat.id } returns chatId
    every { chat.type } returns chatType

    every { message.chat } returns chat
    every { message.messageId } returns messageId

    every { update.message } returns message
    every { update.callbackQuery } returns null

    return CommandContext(update, args)
}
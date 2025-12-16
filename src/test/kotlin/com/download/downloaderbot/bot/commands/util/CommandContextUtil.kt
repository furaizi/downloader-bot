package com.download.downloaderbot.bot.commands.util

import com.download.downloaderbot.bot.commands.CommandContext
import com.github.kotlintelegrambot.entities.Chat
import com.github.kotlintelegrambot.entities.Message
import com.github.kotlintelegrambot.entities.Update

private const val DEFAULT_DATE: Long = 1_700_000_000L

private fun tgChat(
    chatId: Long,
    chatType: String = if (chatId < 0) "group" else "private",
) =
    Chat(
        id = chatId,
        type = chatType
    )

private fun tgMessage(
    chatId: Long,
    messageId: Long,
    text: String? = null,
    chatType: String = if (chatId < 0) "group" else "private",
    date: Long = DEFAULT_DATE,
) =
    Message(
        messageId = messageId,
        date = date,
        chat = tgChat(chatId, chatType),
        text = text,
        entities = emptyList(),
        captionEntities = emptyList(),
        caption = null,
    )

private fun tgUpdate(
    message: Message? = null,
    updateId: Long = 1L,
) =
    Update(
        updateId = updateId,
        message = message,
    )

fun updateDownload(
    url: String,
    chatId: Long,
    messageId: Long,
    updateId: Long = 1L,
) =
    tgUpdate(
        updateId = updateId,
        message = tgMessage(
            chatId = chatId,
            messageId = messageId,
            chatType = "private",
            text = url,
        ),
    )

fun ctx(
    args: List<String> = emptyList(),
    chatId: Long = 123L,
    replyToMessageId: Long = 777L,
    chatType: String = if (chatId < 0) "group" else "private",
    updateId: Long = 1L,
) =
    CommandContext(
        update = tgUpdate(
            updateId = updateId,
            message = tgMessage(
                chatId = chatId,
                messageId = replyToMessageId,
                chatType = chatType,
            ),
        ),
        args = args,
    )


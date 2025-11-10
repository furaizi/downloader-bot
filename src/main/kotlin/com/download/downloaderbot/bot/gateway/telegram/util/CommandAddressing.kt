package com.download.downloaderbot.bot.gateway.telegram.util

import com.github.kotlintelegrambot.entities.Message
import com.github.kotlintelegrambot.entities.MessageEntity
import com.github.kotlintelegrambot.entities.Update
import kotlin.math.min

enum class CommandAddressing {
    NO_COMMAND, OUR, OTHER
}

private fun Message.firstCommandToken(): String? {
    val messageEntity = (entities ?: emptyList())
        .firstOrNull { it.type == MessageEntity.Type.BOT_COMMAND && it.offset == 0 }
        ?: (captionEntities ?: emptyList())
            .firstOrNull { it.type == MessageEntity.Type.BOT_COMMAND && it.offset == 0 }
        ?: return null

    val src = text ?: caption ?: return null
    val end = min(messageEntity.offset + messageEntity.length, src.length)
    if (messageEntity.offset < 0 || end > src.length)
        return null
    return src.substring(messageEntity.offset, end)
}

fun Update.addressing(username: String): CommandAddressing {
    val msg = message
        ?: editedMessage
        ?: channelPost
        ?: editedChannelPost
        ?: return CommandAddressing.NO_COMMAND

    val token = msg.firstCommandToken()
        ?: return CommandAddressing.NO_COMMAND

    if (!token.startsWith("/"))
        return CommandAddressing.NO_COMMAND

    val at = token.indexOf('@')
    if (at < 0)
        return CommandAddressing.OUR

    val mention = token.substring(at + 1)
    return if (mention.equals(username, ignoreCase = true))
        CommandAddressing.OUR
    else
        CommandAddressing.OTHER
}

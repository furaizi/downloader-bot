package com.download.downloaderbot.bot.gateway.telegram.util

import com.github.kotlintelegrambot.entities.Message
import com.github.kotlintelegrambot.entities.MessageEntity
import com.github.kotlintelegrambot.entities.Update

enum class CommandAddressing {
    NO_COMMAND,
    OUR,
    OTHER,
}

private val Update.anyMessage: Message?
    get() = message ?: editedMessage ?: channelPost ?: editedChannelPost

@Suppress("ReturnCount")
fun Update.addressing(username: String): CommandAddressing {
    val token =
        anyMessage
            ?.firstCommandToken()
            ?: return CommandAddressing.NO_COMMAND

    val mention = token.substringAfter('@', "")
    return when {
        mention.isEmpty() -> CommandAddressing.OUR // for all bots
        mention.equals(username, ignoreCase = true) -> CommandAddressing.OUR // for this bot only
        else -> CommandAddressing.OTHER
    }
}

@Suppress("ReturnCount")
private fun Message.firstCommandToken(): String? {
    val content =
        text
            ?: caption
            ?: return null

    val entityList =
        entities
            ?: captionEntities
            ?: return null

    val commandLength =
        entityList
            .firstOrNull { it.type == MessageEntity.Type.BOT_COMMAND && it.offset == 0 }
            ?.length
            ?: return null

    return content.take(commandLength)
}

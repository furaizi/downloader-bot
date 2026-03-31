package com.download.downloaderbot.bot.config

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.Module
import com.fasterxml.jackson.databind.module.SimpleModule
import com.github.kotlintelegrambot.entities.MessageEntity
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class TelegramJacksonConfig {
    @Bean
    fun telegramEnumsModule(): Module =
        SimpleModule("TelegramEnums").apply {
            addDeserializer(
                MessageEntity.Type::class.java,
                object : JsonDeserializer<MessageEntity.Type>() {
                    override fun deserialize(
                        p: JsonParser,
                        ctxt: DeserializationContext,
                    ): MessageEntity.Type = ctxt.parseEnum(p.valueAsString, MessageEntity.Type::class.java)
                },
            )
        }
}

private inline fun <reified T : Enum<T>> DeserializationContext.parseEnum(
    raw: String?,
    enumClass: Class<T>,
): T {
    val value =
        raw
            ?: throw weirdStringException("", enumClass, "Missing enum value")

    val normalized =
        value
            .trim()
            .uppercase()
            .replace('-', '_')

    return try {
        enumValueOf<T>(normalized)
    } catch (_: IllegalArgumentException) {
        throw weirdStringException(value, enumClass, "Unknown enum value")
    }
}

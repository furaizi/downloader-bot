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
                    ): MessageEntity.Type {
                        val raw =
                            p.valueAsString
                                ?: throw ctxt.weirdStringException(
                                    "",
                                    MessageEntity.Type::class.java,
                                    "Missing entity type"
                                )

                        val normalized = raw.trim().uppercase().replace('-', '_')
                        return try {
                            MessageEntity.Type.valueOf(normalized)
                        } catch (_: IllegalArgumentException) {
                            throw ctxt.weirdStringException(
                                raw,
                                MessageEntity.Type::class.java,
                                "Unknown MessageEntity.Type"
                            )
                        }
                    }
                },
            )
        }
}

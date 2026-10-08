package com.download.downloaderbot.bot.config

import com.github.kotlintelegrambot.entities.MessageEntity
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import tools.jackson.core.JsonParser
import tools.jackson.databind.DeserializationContext
import tools.jackson.databind.JacksonModule
import tools.jackson.databind.ValueDeserializer
import tools.jackson.databind.module.SimpleModule

@Configuration
class TelegramJacksonConfig {
    @Bean
    fun telegramEnumsModule(): JacksonModule =
        SimpleModule("TelegramEnums").apply {
            addDeserializer(
                MessageEntity.Type::class.java,
                object : ValueDeserializer<MessageEntity.Type>() {
                    override fun deserialize(
                        p: JsonParser,
                        ctxt: DeserializationContext,
                    ): MessageEntity.Type {
                        val raw =
                            p.valueAsString
                                ?: throw ctxt.weirdStringException(
                                    "",
                                    MessageEntity.Type::class.java,
                                    "Missing entity type",
                                )

                        val normalized = raw.trim().uppercase().replace('-', '_')
                        return try {
                            MessageEntity.Type.valueOf(normalized)
                        } catch (_: IllegalArgumentException) {
                            throw ctxt.weirdStringException(
                                raw,
                                MessageEntity.Type::class.java,
                                "Unknown MessageEntity.Type",
                            )
                        }
                    }
                },
            )
        }
}

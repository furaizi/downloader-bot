package com.download.downloaderbot.bot.config.properties

import jakarta.validation.constraints.NotBlank
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated

@Validated
@ConfigurationProperties(prefix = "telegram.bot")
class BotProperties(
    @field:NotBlank
    val token: String,
    val defaultCommand: String = "download",
    val shareText: String,
    val promoText: String = "\u200B",
    val promoEveryN: Int = 1,
)

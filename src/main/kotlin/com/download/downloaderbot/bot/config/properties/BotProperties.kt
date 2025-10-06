package com.download.downloaderbot.bot.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "telegram.bot")
class BotProperties(
    val token: String,
    val username: String,
    val defaultCommand: String = "download",
    val shareText: String,
    val promoText: String
)

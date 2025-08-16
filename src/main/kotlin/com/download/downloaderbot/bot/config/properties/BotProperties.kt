package com.download.downloaderbot.bot.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "telegram.bot")
class BotProperties(
    val token: String,
    val path: String
) {
}
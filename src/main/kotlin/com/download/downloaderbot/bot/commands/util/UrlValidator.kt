package com.download.downloaderbot.bot.commands.util

import org.springframework.stereotype.Component
import java.net.URI

@Component
class UrlValidator {
    fun isHttpUrl(str: String?): Boolean =
        if (str.isNullOrBlank()) {
            false
        } else {
            try {
                val url = URI(str)
                (url.scheme == "http" || url.scheme == "https") && !url.host.isNullOrBlank()
            } catch (_: Exception) {
                false
            }
        }
}

package com.download.downloaderbot.bot.commands.util

import org.springframework.stereotype.Component
import java.net.URI

@Component
class InputValidator {
    private companion object {
        // Instagram username:
        // - 1..30 chars
        // - allowed: a-z A-Z 0-9 .
        // - no leading/trailing dot
        // - no consecutive dots
        val INSTAGRAM_USERNAME_REGEX =
            Regex("^(?!\\.)(?!.*\\.$)(?!.*\\.\\.)[A-Za-z0-9.]{1,30}$")
    }

    fun isHttpUrl(str: String): Boolean =
        if (str.isBlank()) {
            false
        } else {
            try {
                val url = URI(str)
                (url.scheme == "http" || url.scheme == "https") && !url.host.isNullOrBlank()
            } catch (_: Exception) {
                false
            }
        }

    fun isInstagramUsername(str: String): Boolean =
        if (str.isBlank()) {
            false
        } else {
            INSTAGRAM_USERNAME_REGEX.matches(str.trim())
        }

    fun isInstagramStoriesUrl(str: String): Boolean {
        if (str.isBlank()) {
            return false
        }

        return runCatching {
            val uri = URI(str.trim())
            val host = uri.host?.lowercase()
            val path = uri.path

            val isInstagram = host != null && (host == "instagram.com" || host.endsWith(".instagram.com"))
            isInstagram && path != null && path.startsWith("/stories/")
        }
            .getOrDefault(false)
    }
}

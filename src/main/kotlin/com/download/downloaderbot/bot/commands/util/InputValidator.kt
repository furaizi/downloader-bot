package com.download.downloaderbot.bot.commands.util

import java.net.URI

private val INSTAGRAM_USERNAME_REGEX =
    Regex("^(?!\\.)(?!.*\\.\\.)[A-Za-z0-9._]{1,30}(?<!\\.)$")

fun String.isHttpUrl(): Boolean =
    if (isBlank()) {
        false
    } else {
        try {
            val url = URI(this)
            (url.scheme == "http" || url.scheme == "https") && !url.host.isNullOrBlank()
        } catch (_: Exception) {
            false
        }
    }

fun String.isNotHttpUrl(): Boolean = !isHttpUrl()

fun String.isInstagramUsername(): Boolean =
    if (isBlank()) {
        false
    } else {
        INSTAGRAM_USERNAME_REGEX.matches(trim())
    }

fun String.isNotInstagramUsername(): Boolean = !isInstagramUsername()

fun String.isInstagramStoriesUrl(): Boolean {
    if (isBlank()) {
        return false
    }

    return runCatching {
        val uri = URI(trim())
        val host = uri.host?.lowercase()
        val path = uri.path

        val isInstagram = host != null && (host == "instagram.com" || host.endsWith(".instagram.com"))
        isInstagram && path != null && path.startsWith("/stories/")
    }.getOrDefault(false)
}

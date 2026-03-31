package com.download.downloaderbot.bot.commands.util

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

private val INSTAGRAM_USERNAME_REGEX =
    Regex("^(?!\\.)(?!.*\\.\\.)[A-Za-z0-9._]{1,30}(?<!\\.)$")
private val INSTAGRAM_STORIES_REGEX =
    Regex("""^https?://(?:[a-z0-9-]+\.)*instagram\.com/stories/.*""", RegexOption.IGNORE_CASE)

fun String.isHttpUrl(): Boolean = this.toHttpUrlOrNull() != null

fun String.isNotHttpUrl(): Boolean = !isHttpUrl()

fun String.isInstagramUsername(): Boolean = INSTAGRAM_USERNAME_REGEX.matches(this.trim())

fun String.isNotInstagramUsername(): Boolean = !isInstagramUsername()

fun String.isInstagramStoriesUrl(): Boolean = INSTAGRAM_STORIES_REGEX.matches(this.trim())

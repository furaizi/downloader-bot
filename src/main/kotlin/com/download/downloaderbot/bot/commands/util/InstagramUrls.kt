package com.download.downloaderbot.bot.commands.util

object InstagramUrls {

    private const val BASE = "https://www.instagram.com"

    fun stories(username: String): String =
        "$BASE/stories/$username/"
}
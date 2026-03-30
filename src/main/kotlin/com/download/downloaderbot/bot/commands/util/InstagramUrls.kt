package com.download.downloaderbot.bot.commands.util

data object InstagramUrls {
    private const val BASE = "https://www.instagram.com"

    fun stories(username: String) = "$BASE/stories/$username/"
}

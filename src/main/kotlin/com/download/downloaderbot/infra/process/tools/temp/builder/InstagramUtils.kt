package com.download.downloaderbot.infra.process.tools.temp.builder

object InstagramUtils {
    private val SHORTCODE_REGEX = Regex("""/(?:reels?|p|tv)/([A-Za-z0-9_-]{5,})(?:[/?#]|$)""")

    fun String.instagramShortcode(): String {
        return SHORTCODE_REGEX.find(this)
            ?.groupValues
            ?.get(1) ?: throw IllegalArgumentException("Invalid Instagram URL: $this")
    }
}
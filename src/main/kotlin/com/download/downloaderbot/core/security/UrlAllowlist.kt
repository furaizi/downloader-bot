package com.download.downloaderbot.core.security

interface UrlAllowlist {
    fun isAllowed(url: String): Boolean
}

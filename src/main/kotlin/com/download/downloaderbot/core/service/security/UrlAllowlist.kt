package com.download.downloaderbot.core.service.security

interface UrlAllowlist {
    fun isAllowed(url: String): Boolean
}
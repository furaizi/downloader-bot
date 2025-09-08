package com.download.downloaderbot.core.service.security

import com.download.downloaderbot.core.config.properties.SourceAllowProperties
import org.springframework.stereotype.Component

@Component
class UrlAllowlist(
    private val props: SourceAllowProperties
) {
    private val patterns = props.allow.map { it.toRegex(RegexOption.IGNORE_CASE) }

    fun isAllowed(url: String): Boolean = patterns.any { it.containsMatchIn(url) }
}
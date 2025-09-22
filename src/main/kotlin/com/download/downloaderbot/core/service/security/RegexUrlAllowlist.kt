package com.download.downloaderbot.core.service.security

import com.download.downloaderbot.core.config.properties.SourceAllowProperties
import org.springframework.stereotype.Component

@Component
class RegexUrlAllowlist(
    private val props: SourceAllowProperties
) : UrlAllowlist {
    private val patterns = props.allow.map { it.toRegex(RegexOption.IGNORE_CASE) }

    override fun isAllowed(url: String): Boolean =
        patterns.any { it.containsMatchIn(url) }
}
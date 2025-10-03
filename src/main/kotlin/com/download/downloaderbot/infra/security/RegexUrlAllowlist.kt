package com.download.downloaderbot.infra.security

import com.download.downloaderbot.app.config.properties.SourceAllowProperties
import com.download.downloaderbot.core.security.UrlAllowlist
import org.springframework.stereotype.Component

@Component
class RegexUrlAllowlist(
    private val props: SourceAllowProperties,
) : UrlAllowlist {
    private val patterns = props.allow.map { it.toRegex(RegexOption.IGNORE_CASE) }

    override fun isAllowed(url: String): Boolean = patterns.any { it.containsMatchIn(url) }
}

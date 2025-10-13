package com.download.downloaderbot.infra.source

import java.util.regex.Pattern

data class CompiledSource(
    val name: String,
    val subresources: Map<String, CompiledSubresource>,
)

data class CompiledSubresource(
    val tool: String,
    val patterns: List<Pattern>,
)

data class SourceMatch(
    val source: String,
    val subresource: String,
    val tool: String,
)

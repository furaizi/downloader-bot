package com.download.downloaderbot.app.config.properties

import jakarta.validation.constraints.NotEmpty
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated

@ConfigurationProperties
@Validated
class SourcesProperties(
    val sources: Map<String, SourceDef>,
)

data class SourceDef(
    val enabled: Boolean = true,
    @field:NotEmpty
    val subresources: Map<String, SubresourceDef>,
)

data class SubresourceDef(
    val enabled: Boolean = true,
    val tool: String,
    val format: String = "",
    @field:NotEmpty
    val urlPatterns: List<String>,
)

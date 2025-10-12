package com.download.downloaderbot.infra.source

import jakarta.validation.constraints.NotEmpty
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component
import org.springframework.validation.annotation.Validated

@ConfigurationProperties(prefix = "sources")
@Validated
@Component
class SourcesProperties(
    val sources: Map<String, SourceDef> = emptyMap()
)

data class SourceDef(
    val enabled: Boolean = true,
    @field:NotEmpty
    val subresources: Map<String, SubresourceDef> = emptyMap()
)


data class SubresourceDef(
    val enabled: Boolean = true,
    val tool: String,
    @field:NotEmpty
    val urlPatterns: List<String>
)


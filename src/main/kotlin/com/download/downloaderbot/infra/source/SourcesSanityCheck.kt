package com.download.downloaderbot.infra.source

import com.download.downloaderbot.infra.process.cli.api.ToolRegistry
import jakarta.annotation.PostConstruct
import org.springframework.stereotype.Component

@Component
class SourcesSanityCheck(
    private val sources: SourceRegistry,
    private val tools: ToolRegistry,
    private val props: SourcesProperties
) {
    @PostConstruct
    fun check() {
        val unknown = props.sources
            .filter { it.value.enabled }
            .flatMap { (_, s) -> s.subresources.values.map { it.tool } }
            .distinct()
            .filter { runCatching { tools.get(it) }.isFailure }

        require(unknown.isEmpty()) { "Unknown tools in sources.yml: $unknown" }
        sources.list()
    }
}
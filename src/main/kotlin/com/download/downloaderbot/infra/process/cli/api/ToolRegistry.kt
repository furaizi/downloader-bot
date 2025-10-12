package com.download.downloaderbot.infra.process.cli.api

import org.springframework.stereotype.Component

@Component
class ToolRegistry(
    tools: List<CliTool>
) {
    private val byName: Map<String, CliTool> = tools.associateBy { it.toolId.label } +
        tools.associateBy { it.toolId.name.lowercase().replace('_', '-') }

    fun get(tool: String): CliTool =
        byName[tool] ?: error("Tool not found: $tool. Available: ${byName.keys}")
}
package com.download.downloaderbot.infra.source

import com.download.downloaderbot.core.domain.Media
import com.download.downloaderbot.infra.process.cli.api.CliTool
import com.download.downloaderbot.infra.process.cli.api.ToolId
import com.download.downloaderbot.infra.process.cli.api.ToolRegistry
import io.mockk.every
import io.mockk.mockk
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import

@TestConfiguration(proxyBeanMethods = false)
@Import(
    SourceRegistry::class,
    ToolRegistry::class,
    SourcesSanityCheck::class,
)
class ValidSourcesConfig {
    @Bean
    fun sourcesProperties() =
        buildSourcesProps(
            toolName = "gallery-dl",
        )

    @Bean
    fun galleryDlTool(): CliTool =
        testCliTool(
            label = "gallery-dl",
            name = "GALLERY_DL",
        )
}

@TestConfiguration(proxyBeanMethods = false)
@Import(
    SourceRegistry::class,
    ToolRegistry::class,
    SourcesSanityCheck::class,
)
class InvalidSourcesConfig {
    @Bean
    fun sourcesProperties() =
        buildSourcesProps(
            toolName = "unknown-tool",
        )

    @Bean
    fun galleryDlTool(): CliTool =
        testCliTool(
            label = "gallery-dl",
            name = "GALLERY_DL",
        )
}

private fun buildSourcesProps(toolName: String) =
    sourcesProps {
        source("example-source") {
            sub(
                name = "default",
                tool = toolName,
                format = "mp4",
                patterns = listOf("example\\.com/.*"),
            )
        }
    }

private fun testCliTool(
    label: String,
    name: String,
): CliTool {
    val toolId = mockk<ToolId>()
    every { toolId.label } returns label
    every { toolId.name } returns name

    return object : CliTool {
        override val toolId: ToolId = toolId

        override suspend fun download(
            url: String,
            formatOverride: String,
        ): List<Media> = emptyList()
    }
}

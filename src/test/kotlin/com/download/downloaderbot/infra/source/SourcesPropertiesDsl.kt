package com.download.downloaderbot.infra.source

import com.download.downloaderbot.app.config.properties.SourceDef
import com.download.downloaderbot.app.config.properties.SourcesProperties
import com.download.downloaderbot.app.config.properties.SubresourceDef

/**
* Main entry point: a convenient builder for SourcesProperties for tests.
*
* Example:
*
 * val props = sourcesProps {
 *     source("youtube") {
 *         sub(
 *             name = "videos",
 *             tool = "yt-dlp",
 *             format = "mp4",
 *             patterns = listOf("youtube\\.com/watch"),
 *         )
 *     }
 * }
 */
fun sourcesProps(block: SourcesBuilder.() -> Unit): SourcesProperties =
    SourcesProperties(SourcesBuilder().apply(block).build())

class SourcesBuilder {
    private val sources = linkedMapOf<String, SourceDef>()

    fun source(
        name: String,
        enabled: Boolean = true,
        block: SubresourcesBuilder.() -> Unit,
    ) {
        val subs = SubresourcesBuilder().apply(block).build()
        sources[name] =
            SourceDef(
                enabled = enabled,
                subresources = subs,
            )
    }

    fun build(): Map<String, SourceDef> = sources
}

class SubresourcesBuilder {
    private val subs = linkedMapOf<String, SubresourceDef>()

    fun sub(
        name: String,
        tool: String,
        format: String = "",
        enabled: Boolean = true,
        patterns: List<String>,
    ) {
        subs[name] =
            SubresourceDef(
                enabled = enabled,
                tool = tool,
                format = format,
                urlPatterns = patterns,
            )
    }

    fun build(): Map<String, SubresourceDef> = subs
}

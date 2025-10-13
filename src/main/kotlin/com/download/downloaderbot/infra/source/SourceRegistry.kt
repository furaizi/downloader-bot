package com.download.downloaderbot.infra.source

import com.download.downloaderbot.app.config.properties.SourcesProperties
import org.springframework.stereotype.Service
import java.util.concurrent.atomic.AtomicReference
import java.util.regex.Pattern

@Service
class SourceRegistry(
    props: SourcesProperties,
) {
    private val compiled = AtomicReference(compile(props))

    fun match(url: String): SourceMatch? {
        val snapshot = compiled.get()
        for (src in snapshot) {
            for ((subName, sub) in src.subresources) {
                if (sub.patterns.any { it.matcher(url).find() }) {
                    return SourceMatch(src.name, subName, sub.tool)
                }
            }
        }
        return null
    }

    fun list(): List<CompiledSource> = compiled.get()

    fun reload(newProps: SourcesProperties) {
        compiled.set(compile(newProps))
    }

    private fun compile(props: SourcesProperties): List<CompiledSource> {
        return props.sources
            .filter { (_, def) -> def.enabled }
            .map { (name, def) ->
                val compiledSubs =
                    def.subresources
                        .filter { (_, sub) -> sub.enabled }
                        .mapValues { (_, sub) ->
                            CompiledSubresource(
                                tool = sub.tool,
                                patterns = sub.urlPatterns.map { Pattern.compile(it) },
                            )
                        }
                CompiledSource(name = name, subresources = compiledSubs)
            }
    }
}

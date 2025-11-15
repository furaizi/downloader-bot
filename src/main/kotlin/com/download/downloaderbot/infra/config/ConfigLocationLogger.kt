package com.download.downloaderbot.infra.config

import mu.KotlinLogging
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.annotation.Configuration
import org.springframework.context.event.EventListener
import org.springframework.core.env.ConfigurableEnvironment

private val log = KotlinLogging.logger {}

@Configuration
class ConfigLocationLogger(
    private val environment: ConfigurableEnvironment,
) {
    @EventListener(ApplicationReadyEvent::class)
    fun logConfigSources() {
        val sources =
            environment.propertySources
                .iterator()
                .asSequence()
                .toList()

        val relevantNames =
            sources
                .map { it.name }
                .filter { name ->
                    name.contains("application", ignoreCase = true) ||
                        name.contains("Config resource", ignoreCase = true)
                }

        if (relevantNames.isEmpty()) {
            log.info("No application configuration property sources detected.")
            return
        }

        log.info("Resolved application configuration property sources (highest precedence first):")
        relevantNames.forEachIndexed { index, name ->
            log.info("  ${index + 1}. $name")
        }

        val usesExternal = relevantNames.any { it.contains("file:") }
        if (usesExternal) {
            log.info("External configuration detected (file-based application.yml overrides packaged defaults).")
        } else {
            log.info("Using only packaged application.yml from the JAR (no external application.yml found).")
        }
    }
}

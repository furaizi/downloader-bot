package com.download.downloaderbot.core.storage

import com.download.downloaderbot.core.config.properties.MediaProperties
import mu.KotlinLogging
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.SchedulingConfigurer
import org.springframework.scheduling.config.ScheduledTaskRegistrar
import org.springframework.scheduling.support.PeriodicTrigger

private val log = KotlinLogging.logger {}

@Configuration
@EnableScheduling
class MediaCleanupSchedulingConfig(
    private val service: MediaCleanupService,
    private val props: MediaProperties
) : SchedulingConfigurer {

    override fun configureTasks(registrar: ScheduledTaskRegistrar) {
        val trigger = PeriodicTrigger(props.cleanup.interval)
        trigger.setInitialDelay(props.cleanup.initialDelay)

        registrar.addTriggerTask({
            val report = service.cleanup()
            if (report.deletedFiles > 0)
                log.info { "Media cleanup removed ${report.deletedFiles} files and freed ${report.freedBytes} bytes" }
            else
                log.debug { "Media cleanup run completed without deletions" }
        }, trigger)
    }
}
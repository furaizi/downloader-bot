package com.download.downloaderbot.infra.cleanup

import com.download.downloaderbot.app.config.properties.MediaProperties
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
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
    private val props: MediaProperties,
    private val maintenanceScope: CoroutineScope,
) : SchedulingConfigurer {
    private val running = java.util.concurrent.atomic.AtomicBoolean(false)

    override fun configureTasks(registrar: ScheduledTaskRegistrar) {
        val trigger =
            PeriodicTrigger(props.cleanup.interval).apply {
                setInitialDelay(props.cleanup.initialDelay)
            }

        registrar.addTriggerTask({
            maintenanceScope.launch {
                runCleanupGuarded()
            }
        }, trigger)
    }

    private suspend fun runCleanupGuarded() {
        if (!running.compareAndSet(false, true)) {
            log.debug { "Media cleanup skipped: previous run is still in progress" }
            return
        }
        try {
            val report = service.cleanup()
            if (report.deletedFiles > 0) {
                log.info { "Media cleanup removed ${report.deletedFiles} files and freed ${report.freedBytes} bytes" }
            } else {
                log.debug { "Media cleanup run completed without deletions" }
            }
        } catch (t: Throwable) {
            log.warn(t) { "Media cleanup failed" }
        } finally {
            running.set(false)
        }
    }
}

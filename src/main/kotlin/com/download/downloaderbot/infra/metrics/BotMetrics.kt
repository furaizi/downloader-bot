package com.download.downloaderbot.infra.metrics

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import org.springframework.stereotype.Component

@Component
class BotMetrics(val registry: MeterRegistry) {
    val updates: Counter =
        Counter.builder("downloader_updates_total")
            .description("Total Telegram updates processed")
            .register(registry)

    val errors: Counter =
        Counter.builder("downloader_errors_total")
            .description("Total errors while processing updates")
            .register(registry)

    fun commandCounter(command: String): Counter =
        Counter.builder("downloader_commands_total")
            .description("Commands processed")
            .tag("command", command)
            .register(registry)

    fun commandTimer(command: String): Timer =
        Timer.builder("downloader_update_duration_seconds")
            .description("Update/command processing duration")
            .tag("command", command)
            .publishPercentileHistogram()
            .register(registry)

    fun jobQueueDelayTimer(job: String): Timer =
        Timer.builder("downloader_job_queue_delay_seconds")
            .description("Job queue delay before execution")
            .tag("job", job)
            .publishPercentileHistogram()
            .register(registry)

    fun jobDurationTimer(job: String): Timer =
        Timer.builder("downloader_job_duration_seconds")
            .description("Job execution duration")
            .tag("job", job)
            .publishPercentileHistogram()
            .register(registry)

    fun jobTotalDurationTimer(job: String): Timer =
        Timer.builder("downloader_job_total_duration_seconds")
            .description("Total job duration (queue + execution)")
            .tag("job", job)
            .publishPercentileHistogram()
            .register(registry)
}

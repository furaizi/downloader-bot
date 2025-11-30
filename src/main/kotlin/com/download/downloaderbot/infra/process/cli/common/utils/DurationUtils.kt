package com.download.downloaderbot.infra.process.cli.common.utils

import java.util.Locale
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

private const val MILLIS_PER_SECOND = 1_000.0
private const val NANOS_PER_MILLISECOND = 1_000_000

/*
  <1s  -> "123ms"
  <1m  -> "2.137s"
  <1h  -> "01:12.345"
  >=1h -> "1:05:07.123"
 */
fun Duration.human(): String =
    when {
        this < 1.toDuration(DurationUnit.SECONDS) -> "${inWholeMilliseconds}ms"

        this < 1.toDuration(DurationUnit.MINUTES) -> {
            val s = inWholeMilliseconds / MILLIS_PER_SECOND
            String.format(Locale.ROOT, "%.3fs", s)
        }

        this < 1.toDuration(DurationUnit.HOURS) -> {
            toComponents { _, minutes, seconds, nanoseconds ->
                val millis = nanoseconds / NANOS_PER_MILLISECOND
                String.format(Locale.ROOT, "%02d:%02d.%03d", minutes, seconds, millis)
            }
        }

        else -> {
            toComponents { hours, minutes, seconds, nanoseconds ->
                val millis = nanoseconds / NANOS_PER_MILLISECOND
                String.format(Locale.ROOT, "%d:%02d:%02d.%03d", hours, minutes, seconds, millis)
            }
        }
    }

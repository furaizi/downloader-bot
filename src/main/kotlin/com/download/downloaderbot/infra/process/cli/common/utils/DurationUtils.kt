package com.download.downloaderbot.infra.process.cli.common.utils

import java.util.Locale
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

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
            val s = inWholeMilliseconds / 1000.0
            String.format(Locale.ROOT, "%.3fs", s)
        }

        this < 1.toDuration(DurationUnit.HOURS) -> {
            toComponents { _, minutes, seconds, nanoseconds ->
                val millis = nanoseconds / 1_000_000
                String.format(Locale.ROOT, "%02d:%02d.%03d", minutes, seconds, millis)
            }
        }

        else -> {
            toComponents { hours, minutes, seconds, nanoseconds ->
                val millis = nanoseconds / 1_000_000
                String.format(Locale.ROOT, "%d:%02d:%02d.%03d", hours, minutes, seconds, millis)
            }
        }
    }

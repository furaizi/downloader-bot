package com.download.downloaderbot.infra.media.path.naming

import java.net.URI
import java.time.Instant
import java.util.UUID

object BasePrefixGenerator {
    private const val SHORT_UUID_LENGTH = 8

    fun generate(url: String): String {
        val host = getHostName(url) ?: "media"
        val timestamp = Instant.now().toEpochMilli()
        val shortUuid =
            UUID.randomUUID()
                .toString()
                .take(SHORT_UUID_LENGTH)
        return "$host-$timestamp-$shortUuid"
    }

    private fun getHostName(url: String): String? {
        return runCatching {
            URI(url).host
                ?.replace(oldValue = ":", newValue = "-")
        }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }
    }
}

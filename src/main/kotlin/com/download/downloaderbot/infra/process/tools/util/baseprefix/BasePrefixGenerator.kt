package com.download.downloaderbot.infra.process.tools.util.baseprefix

import java.net.URI
import java.time.Instant
import java.util.UUID

object BasePrefixGenerator {

    fun generate(url: String): String {
        val host = getHostName(url) ?: "media"
        val timestamp = Instant.now().toEpochMilli()
        val shortUuid = UUID.randomUUID()
            .toString()
            .take(8)
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
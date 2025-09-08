package com.download.downloaderbot.core.queue

import java.time.Instant
import java.util.UUID

enum class JobStatus {
    QUEUED, DOWNLOADING, DONE, FAILED
}

data class DownloadJob(
    val id: UUID = UUID.randomUUID(),
    val chatId: Long,
    val url: String,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
    val status: JobStatus = JobStatus.QUEUED,
    val error: String? = null,
    val outputPath: String? = null
)
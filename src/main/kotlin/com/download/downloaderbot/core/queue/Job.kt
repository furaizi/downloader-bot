package com.download.downloaderbot.core.queue

import com.download.downloaderbot.core.domain.Media
import com.download.downloaderbot.core.domain.MediaType
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
    val startedAt: Instant? = null,
    val finishedAt: Instant? = null,
    val status: JobStatus = JobStatus.QUEUED,
    val error: String? = null,
    val resultCount: Int? = null,
    val firstMediaType: MediaType? = null
)

data class JobSucceededEvent(val job: DownloadJob, val media: List<Media>)
data class JobFailedEvent(val job: DownloadJob, val error: String)
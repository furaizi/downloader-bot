package com.download.downloaderbot.core.queue

import com.download.downloaderbot.core.downloader.MediaDownloader
import jakarta.annotation.PreDestroy
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import mu.KotlinLogging
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID

private val log = KotlinLogging.logger {}

data class EnqueueResult(val jobId: UUID)

@Service
class DownloadQueueService(
    private val jobStore: JobStore,
    private val downloader: MediaDownloader,
    private val publisher: ApplicationEventPublisher,
    parentScope: CoroutineScope
) {
    private val scope = CoroutineScope(parentScope.coroutineContext + SupervisorJob())
    private val queue = Channel<UUID>(capacity = Channel.UNLIMITED)
    private val workerCount = 3

    init {
        repeat(workerCount) { idx ->
            scope.launch {
                launchWorker(idx)
            }
        }
    }

    private suspend fun launchWorker(workerIndex: Int) {
        log.info { "Download worker-$workerIndex started" }
        for (jobId in queue) {
            val job = jobStore.get(jobId) ?: continue

            jobStore.update(jobId) {
                it.copy(status = JobStatus.DOWNLOADING, error = null, startedAt = Instant.now())
            }

            try {
                val media = downloader.download(job.url)

                jobStore.update(jobId) {
                    it.copy(
                        status = JobStatus.DONE,
                        finishedAt = Instant.now(),
                        resultCount = media.size,
                        firstMediaType = media.firstOrNull()?.type
                    )
                }
                publisher.publishEvent(JobSucceededEvent(jobStore.get(jobId)!!, media))
                log.info { "Job $jobId finished: items=${media.size}, firstType=${media.firstOrNull()?.type}" }

            } catch (ex: CancellationException) {
                jobStore.update(jobId) {
                    it.copy(status = JobStatus.FAILED, error = "Cancelled", finishedAt = Instant.now())
                }
                publisher.publishEvent(JobFailedEvent(jobStore.get(jobId)!!, "Cancelled"))
                log.warn(ex) { "Job $jobId cancelled" }

            } catch (ex: Exception) {
                val msg = ex.message ?: "Download error"
                jobStore.update(jobId) {
                    it.copy(status = JobStatus.FAILED, error = msg, finishedAt = Instant.now())
                }
                publisher.publishEvent(JobFailedEvent(jobStore.get(jobId)!!, msg))
                log.error(ex) { "Job $jobId failed" }
            }
        }
    }

    fun enqueue(chatId: Long, url: String): EnqueueResult {
        val job = jobStore.create(DownloadJob(chatId = chatId, url = url))
        queue.trySend(job.id)
        return EnqueueResult(job.id)
    }

    fun getStatus(jobId: UUID): DownloadJob? = jobStore.get(jobId)

    fun listRecent(chatId: Long, limit: Int = 10): List<DownloadJob> = jobStore.listByChat(chatId, limit)

    @PreDestroy
    fun shutdown() {
        scope.cancel()
        queue.close()
        log.info { "DownloadQueueService shutdown complete" }
    }

}
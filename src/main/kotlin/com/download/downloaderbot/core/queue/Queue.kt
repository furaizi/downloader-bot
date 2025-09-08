package com.download.downloaderbot.core.queue

import com.download.downloaderbot.core.downloader.MediaDownloader
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import mu.KotlinLogging
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID

private val log = KotlinLogging.logger {}

data class EnqueueResult(val jobId: UUID)

@Service
class DownloadQueueService(
    private val jobStore: JobStore,
    private val downloader: MediaDownloader
) : CoroutineScope {

    private val supervisor = SupervisorJob()
    override val coroutineContext = Dispatchers.IO + supervisor

    private val queue = Channel<UUID>(capacity = Channel.UNLIMITED)
    private val workerCount = 3

    init {
        repeat(workerCount) { idx ->
            launchWorker(idx)
        }
    }

    private fun launchWorker(workerIndex: Int) = launch {
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
                log.info { "Job $jobId finished: items=${media.size}, firstType=${media.firstOrNull()?.type}" }
            } catch (ex: CancellationException) {
                jobStore.update(jobId) {
                    it.copy(status = JobStatus.FAILED, error = "Cancelled", finishedAt = Instant.now())
                }
                log.warn(ex) { "Job $jobId cancelled" }
            } catch (ex: Exception) {
                jobStore.update(jobId) {
                    it.copy(status = JobStatus.FAILED, error = ex.message, finishedAt = Instant.now())
                }
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

    fun shutdown() {
        supervisor.cancel()
        queue.close()
    }

}
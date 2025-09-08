package com.download.downloaderbot.core.queue

import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

interface JobStore {
    fun create(job: DownloadJob): DownloadJob
    fun update(id: UUID, mutator: (DownloadJob) -> DownloadJob): DownloadJob?
    fun get(id: UUID): DownloadJob?
    fun listByChat(chatId: Long, limit: Int = 10): List<DownloadJob>
}

class InMemoryJobStore : JobStore {
    private val store = ConcurrentHashMap<UUID, DownloadJob>()

    override fun create(job: DownloadJob): DownloadJob {
        store[job.id] = job
        return job
    }

    override fun update(id: UUID, mutator: (DownloadJob) -> DownloadJob): DownloadJob? {
        return store.computeIfPresent(id) { _, old ->
            mutator(old).copy(updatedAt = Instant.now())
        }
    }

    override fun get(id: UUID): DownloadJob? = store[id]

    override fun listByChat(chatId: Long, limit: Int): List<DownloadJob> =
        store.values.asSequence()
            .filter { it.chatId == chatId }
            .sortedByDescending { it.createdAt }
            .take(limit)
            .toList()
}
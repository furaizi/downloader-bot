package com.download.downloaderbot.bot.commands

import com.download.downloaderbot.core.queue.DownloadQueueService
import com.download.downloaderbot.core.queue.JobStatus
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class StatusCommand(
    private val queue: DownloadQueueService,
    gateway: TelegramGateway
) : CommandHandler(gateway) {

    override val name = "status"

    override suspend fun handle(ctx: CommandContext) {
        val arg = ctx.args.firstOrNull()?.trim()

        if (arg.isNullOrEmpty()) {
            val recent = queue.listRecent(ctx.chatId, limit = 10)
            if (recent.isEmpty()) {
                gateway.replyText(ctx.chatId, "No jobs found.")
                return
            }

            val text = buildString {
                appendLine("Recent download jobs:")
                recent.forEachIndexed { i, job ->
                    appendLine("${i + 1}. ${job.url} - ${statusText(job.status)}")
                }
            }
            gateway.replyText(ctx.chatId, text)
            return
        }

        val id = runCatching { UUID.fromString(arg) }.getOrNull()
        if (id == null) {
            gateway.replyText(ctx.chatId, "Invalid jobId format.")
            return
        }

        val job = queue.getStatus(id)
        if (job == null) {
            gateway.replyText(ctx.chatId, "Job not found.")
            return
        }

        val text = buildString {
            appendLine("Job status:")
            appendLine("1. ${job.url} - ${statusText(job.status)}")
        }
        gateway.replyText(ctx.chatId, text)
    }

    private fun statusText(status: JobStatus) = when (status) {
        JobStatus.QUEUED -> "Queued"
        JobStatus.DOWNLOADING -> "Downloading..."
        JobStatus.DONE -> "Success"
        JobStatus.FAILED -> "Failed"
    }
}

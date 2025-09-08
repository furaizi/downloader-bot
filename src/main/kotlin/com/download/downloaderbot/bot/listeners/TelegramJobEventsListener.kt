package com.download.downloaderbot.bot.listeners

import com.download.downloaderbot.bot.commands.TelegramGateway
import com.download.downloaderbot.core.domain.Media
import com.download.downloaderbot.core.domain.MediaType
import com.download.downloaderbot.core.queue.JobFailedEvent
import com.download.downloaderbot.core.queue.JobSucceededEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import java.io.File

@Component
class TelegramJobEventsListener(
    private val gateway: TelegramGateway,
    private val scope: CoroutineScope
) {

    @EventListener
    fun onSuccess(e: JobSucceededEvent) {
        scope.launch {
            sendMedia(e.job.chatId, e.media)
        }
    }

    @EventListener
    fun onFail(e: JobFailedEvent) {
        scope.launch {
            gateway.replyText(e.job.chatId, "❌ Download failed: ${e.error}")
        }
    }

    private suspend fun sendMedia(chatId: Long, mediaList: List<Media>) {
        val firstMedia = mediaList.first()

        if (firstMedia.type == MediaType.IMAGE && mediaList.size in 2..10) {
            val files = mediaList.map { File(it.fileUrl) }
            gateway.sendPhotosAlbum(chatId, files)
        }
        else if (firstMedia.type == MediaType.IMAGE && mediaList.size > 10) {
            val files = mediaList.map { File(it.fileUrl) }
            gateway.sendPhotosAlbumChunked(chatId, files)
        }
        else {
            mediaList.forEach {
                val file = File(it.fileUrl)
                when (it.type) {
                    MediaType.VIDEO -> gateway.sendVideo(chatId, file)
                    MediaType.AUDIO -> gateway.sendAudio(chatId, file)
                    MediaType.IMAGE -> gateway.sendPhoto(chatId, file)
                }
            }
        }
    }
}
package com.download.downloaderbot.bot.controller

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.Update
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class WebhookBotController(
    private val bot: Bot,
    private val botScope: CoroutineScope
) {

    @PostMapping("/telegram")
    suspend fun onUpdate(@RequestBody update: Update) {
        botScope.launch {
            bot.processUpdate(update)
        }
    }
}
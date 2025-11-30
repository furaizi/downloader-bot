package com.download.downloaderbot.bot.core

import com.github.kotlintelegrambot.entities.Update

fun interface UpdateHandler {
    suspend fun handle(update: Update)
}
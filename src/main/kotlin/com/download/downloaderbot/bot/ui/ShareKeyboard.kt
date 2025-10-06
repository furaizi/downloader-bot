package com.download.downloaderbot.bot.ui

import com.github.kotlintelegrambot.entities.InlineKeyboardMarkup
import com.github.kotlintelegrambot.entities.keyboard.InlineKeyboardButton
import java.net.URLEncoder

fun shareKeyboard(
    botUsername: String,
    text: String,
): InlineKeyboardMarkup {
    val url = "https://t.me/$botUsername"
    val encodedUrl = URLEncoder.encode(url, Charsets.UTF_8)
    val encodedText = URLEncoder.encode(text, Charsets.UTF_8)

    val shareLink = "https://t.me/share/url?url=$encodedUrl&text=$encodedText"
    val addToGroupLink = "https://t.me/$botUsername?startgroup=true"

    return InlineKeyboardMarkup.createSingleRowKeyboard(
        InlineKeyboardButton.Url("🔗 Поділитись ботом", shareLink),
        InlineKeyboardButton.Url("👥 Додати в групу", addToGroupLink),
    )
}

package com.download.downloaderbot

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class DownloaderBotApplication

fun main(args: Array<String>) {
    runApplication<DownloaderBotApplication>(*args)
}

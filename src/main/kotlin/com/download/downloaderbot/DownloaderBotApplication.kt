package com.download.downloaderbot

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@ConfigurationPropertiesScan
@SpringBootApplication
class DownloaderBotApplication

@Suppress("SpreadOperator")
fun main(args: Array<String>) {
    runApplication<DownloaderBotApplication>(*args)
}

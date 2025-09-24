package com.download.downloaderbot.app.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "downloader.gallery-dl")
data class GalleryDlProperties(
    val bin: String,
    val extraArgs: List<String> = emptyList(),
    val timeout: Duration = Duration.ofMinutes(1)
)

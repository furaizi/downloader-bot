package com.download.downloaderbot.infra.config.providers

import com.download.downloaderbot.core.downloader.MediaProvider
import com.download.downloaderbot.infra.config.containsAll
import com.download.downloaderbot.infra.config.tools.YtDlpConfig
import com.download.downloaderbot.infra.media.provider.BaseMediaProvider
import com.download.downloaderbot.infra.process.cli.api.CliTool
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class YoutubeProvidersConfig {
    @Bean
    @ConditionalOnBean(YtDlpConfig::class)
    fun youtubeShortsDownloader(ytDlp: CliTool): MediaProvider =
        BaseMediaProvider(ytDlp) {
            it.containsAll("youtube.com", "shorts")
        }
}

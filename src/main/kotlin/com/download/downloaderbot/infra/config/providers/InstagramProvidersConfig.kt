package com.download.downloaderbot.infra.config.providers

import com.download.downloaderbot.app.config.properties.MediaProperties
import com.download.downloaderbot.core.downloader.MediaProvider
import com.download.downloaderbot.infra.config.containsAll
import com.download.downloaderbot.infra.config.tools.YtDlpConfig
import com.download.downloaderbot.infra.di.ForYtDlp
import com.download.downloaderbot.infra.media.files.FilesByPrefixFinder
import com.download.downloaderbot.infra.media.path.PathGenerator
import com.download.downloaderbot.infra.media.provider.BaseMediaProvider
import com.download.downloaderbot.infra.process.cli.api.CliTool
import com.download.downloaderbot.infra.process.cli.ytdlp.YtDlpMedia
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class InstagramProvidersConfig(
    val props: MediaProperties
) {

    @Bean
    @ConditionalOnBean(YtDlpConfig::class)
    fun instagramReelsDownloader(
        ytDlp: CliTool<YtDlpMedia>,
        ytDlpPathGenerator: PathGenerator,
        @ForYtDlp filesByPrefixFinder: FilesByPrefixFinder
    ): MediaProvider =
        BaseMediaProvider(props, ytDlp, ytDlpPathGenerator, filesByPrefixFinder, urlPredicate = {
            it.containsAll("instagram.com", "reel")
        })

}
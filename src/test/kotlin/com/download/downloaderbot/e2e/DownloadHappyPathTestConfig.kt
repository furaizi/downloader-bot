package com.download.downloaderbot.e2e

import com.download.downloaderbot.bot.gateway.RecordingBotPort
import com.download.downloaderbot.infra.di.ForYtDlp
import com.download.downloaderbot.infra.network.StubFinalUrlResolver
import com.download.downloaderbot.infra.process.runner.FakeProcessRunner
import com.download.downloaderbot.infra.process.runner.ProcessRunner
import com.download.downloaderbot.core.domain.Media
import com.download.downloaderbot.infra.process.cli.api.CliTool
import com.download.downloaderbot.infra.process.cli.api.ToolId
import com.download.downloaderbot.core.net.FinalUrlResolver
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary

@TestConfiguration
class DownloadHappyPathTestConfig {
    @Bean
    @Primary
    fun recordingBotPort() = RecordingBotPort()

    @Bean
    @Primary
    fun stubFinalUrlResolver(): FinalUrlResolver = StubFinalUrlResolver()

    @Bean
    @Primary
    @ForYtDlp
    fun fakeYtDlpRunner(): ProcessRunner = FakeProcessRunner()

//    @Bean
//    fun fakeGalleryCliTool(): CliTool =
//        object : CliTool {
//            override val toolId: ToolId = ToolId.GALLERY_DL
//            override suspend fun download(
//                url: String,
//                formatOverride: String,
//            ): List<Media> = emptyList()
//        }
}

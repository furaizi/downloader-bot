package com.download.downloaderbot.e2e.config

import com.download.downloaderbot.bot.gateway.RecordingBotPort
import com.download.downloaderbot.core.net.FinalUrlResolver
import com.download.downloaderbot.infra.di.ForYtDlp
import com.download.downloaderbot.infra.network.StubFinalUrlResolver
import com.download.downloaderbot.infra.process.runner.FailingProcessRunner
import com.download.downloaderbot.infra.process.runner.FakeProcessRunner
import com.download.downloaderbot.infra.process.runner.ProcessRunner
import com.download.downloaderbot.infra.process.runner.SleepyFakeProcessRunner
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary

@TestConfiguration
class E2ETestConfig {
    @Bean
    @Primary
    fun recordingBotPort() = RecordingBotPort()

    @Bean
    @Primary
    fun stubFinalUrlResolver(): FinalUrlResolver = StubFinalUrlResolver()

    @Bean
    @Primary
    @ForYtDlp
    @ConditionalOnProperty(name = ["downloader.yt-dlp.runner"], havingValue = "fake", matchIfMissing = true)
    fun fakeYtDlpRunner(): ProcessRunner = FakeProcessRunner()

    @Bean
    @Primary
    @ForYtDlp
    @ConditionalOnProperty(name = ["downloader.yt-dlp.runner"], havingValue = "fail")
    fun failingYtDlpRunner(): ProcessRunner = FailingProcessRunner()

    @Bean
    @Primary
    @ForYtDlp
    @ConditionalOnProperty(name = ["downloader.yt-dlp.runner"], havingValue = "sleepy")
    fun sleepyYtDlpRunner(): ProcessRunner = SleepyFakeProcessRunner()
}
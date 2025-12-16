package com.download.downloaderbot.infra.process.runner

import kotlinx.coroutines.delay

class SleepyFakeProcessRunner(
    private val delayMillis: Long = 100,
    private val delegate: FakeProcessRunner = FakeProcessRunner(),
) : ProcessRunner {
    override suspend fun run(
        args: List<String>,
        url: String,
    ): String {
        delay(delayMillis)
        return delegate.run(args, url)
    }
}

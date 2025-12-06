package com.download.downloaderbot.infra.source

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import org.springframework.boot.test.context.runner.ApplicationContextRunner

class SourcesSanityCheckIT : FunSpec({

    val validContextRunner =
        ApplicationContextRunner()
            .withUserConfiguration(ValidSourcesConfig::class.java)

    val invalidContextRunner =
        ApplicationContextRunner()
            .withUserConfiguration(InvalidSourcesConfig::class.java)

    test("context starts when all tools are known") {
        validContextRunner.run { ctx ->
            ctx.startupFailure.shouldBeNull()

            ctx.getBean(SourcesSanityCheck::class.java).shouldNotBeNull()

            val registry = ctx.getBean(SourceRegistry::class.java)
            registry.list().shouldNotBeEmpty()
        }
    }

    test("context fails to start when there are unknown tools in sources") {
        invalidContextRunner.run { ctx ->
            val failure = ctx.startupFailure
            failure.shouldNotBeNull()

            val rootCause = failure.rootCause()
            rootCause.shouldBeInstanceOf<IllegalArgumentException>()
            rootCause.message.shouldContain("Unknown tools in sources.yml")
            rootCause.message.shouldContain("unknown-tool")
        }
    }
})

private fun Throwable.rootCause(): Throwable = generateSequence(this) { it.cause }.last()

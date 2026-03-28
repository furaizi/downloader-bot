package com.download.downloaderbot.infra.process.utils

import io.kotest.core.annotation.Condition
import io.kotest.core.spec.Spec
import java.nio.file.Files
import java.nio.file.Path
import kotlin.reflect.KClass

class PosixShellCondition : Condition {
    override fun evaluate(kclass: KClass<out Spec>): Boolean {
        val os = System.getProperty("os.name").lowercase()
        return !os.contains("win") && Files.isExecutable(Path.of("/bin/sh"))
    }
}

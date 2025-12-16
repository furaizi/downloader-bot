package com.download.downloaderbot.util

import io.kotest.core.spec.style.scopes.FunSpecContainerScope
import io.kotest.matchers.shouldBe

suspend fun <T, R> FunSpecContainerScope.case(
    name: String,
    input: T,
    expected: R,
    transform: (T) -> R,
) {
    test(name) {
        transform(input) shouldBe expected
    }
}

package com.download.downloaderbot.infra.providers.interfaces

fun interface ProbeValidator<in META> {
    fun validate(url: String, meta: META)
}
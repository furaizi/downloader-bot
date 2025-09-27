package com.download.downloaderbot.infra.media.validation

fun interface ProbeValidator<in META> {
    fun validate(url: String, meta: META)
}
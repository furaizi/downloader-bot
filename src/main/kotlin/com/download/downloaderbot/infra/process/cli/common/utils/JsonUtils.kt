package com.download.downloaderbot.infra.process.cli.common.utils

fun String.preview(length: Int = 200): String =
    if (this.length > length)
        "${take(length)}..."
    else this
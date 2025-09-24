package com.download.downloaderbot.infra.process.tools.instaloader

import com.download.downloaderbot.app.config.properties.InstaloaderProperties
import com.download.downloaderbot.infra.process.tools.AbstractCliTool

class Instaloader(
    val config: InstaloaderProperties
) : AbstractCliTool(config.bin, config.timeout) {
}
package com.download.downloaderbot.infra.process.cli.base

import com.download.downloaderbot.app.config.properties.MediaProperties
import com.download.downloaderbot.infra.media.files.DirectoryFilesByPrefixFinder
import com.download.downloaderbot.infra.media.files.SingleFileByPrefixFinder
import com.download.downloaderbot.infra.media.path.GalleryDlPathGenerator
import com.download.downloaderbot.infra.media.path.YtDlpPathGenerator
import com.download.downloaderbot.infra.process.cli.api.ToolId
import com.download.downloaderbot.infra.process.cli.api.interfaces.CommandBuilder
import com.download.downloaderbot.infra.process.cli.common.extractor.OutputJsonExtractor
import com.download.downloaderbot.infra.process.cli.common.parser.DefaultJsonParser
import com.download.downloaderbot.infra.process.cli.ytdlp.YtDlpMedia
import com.download.downloaderbot.infra.process.runner.DefaultProcessRunner
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.time.Duration

class CliToolFixture(private val props: MediaProperties) {

    fun ytDlp(cmd: CommandBuilder) = BaseCliTool(
        props = props,
        pathGenerator = YtDlpPathGenerator(props),
        cmdBuilder = cmd,
        runner = DefaultProcessRunner("/bin/sh", Duration.ofSeconds(3)),
        jsonExtractor = OutputJsonExtractor("test-cli"),
        jsonParser = DefaultJsonParser(jacksonObjectMapper(), object : TypeReference<YtDlpMedia>() {}),
        fileFinder = SingleFileByPrefixFinder(),
        toolId = ToolId.YT_DLP,
    )

    fun galleryDl(cmd: CommandBuilder) = NoMetadataCliTool(
        props = props,
        pathGenerator = GalleryDlPathGenerator(props),
        cmdBuilder = cmd,
        runner = DefaultProcessRunner("/bin/sh", Duration.ofSeconds(3)),
        fileFinder = DirectoryFilesByPrefixFinder(),
        toolId = ToolId.GALLERY_DL,
    )

}
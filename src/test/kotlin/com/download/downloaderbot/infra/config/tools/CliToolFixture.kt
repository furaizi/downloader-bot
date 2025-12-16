package com.download.downloaderbot.infra.config.tools

import com.download.downloaderbot.app.config.properties.MediaProperties
import com.download.downloaderbot.infra.media.files.DirectoryFilesByPrefixFinder
import com.download.downloaderbot.infra.media.files.SingleFileByPrefixFinder
import com.download.downloaderbot.infra.media.path.GalleryDlPathGenerator
import com.download.downloaderbot.infra.media.path.YtDlpPathGenerator
import com.download.downloaderbot.infra.process.cli.api.ToolId
import com.download.downloaderbot.infra.process.cli.api.interfaces.CommandBuilder
import com.download.downloaderbot.infra.process.cli.base.BaseCliTool
import com.download.downloaderbot.infra.process.cli.base.NoMetadataCliTool
import com.download.downloaderbot.infra.process.cli.common.extractor.OutputJsonExtractor
import com.download.downloaderbot.infra.process.cli.common.parser.DefaultJsonParser
import com.download.downloaderbot.infra.process.cli.ytdlp.YtDlpMedia
import com.download.downloaderbot.infra.process.runner.DefaultProcessRunner
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.time.Duration

class CliToolFixture(private val props: MediaProperties) {
    private val runner = DefaultProcessRunner("/bin/sh", Duration.ofSeconds(3))
    private val extractor = OutputJsonExtractor("test-cli")
    private val mapper = jacksonObjectMapper()
    private val ytDlpParser = DefaultJsonParser(mapper, object : TypeReference<YtDlpMedia>() {})

    fun ytDlp(cmd: CommandBuilder) =
        BaseCliTool(
            props = props,
            pathGenerator = YtDlpPathGenerator(props),
            cmdBuilder = cmd,
            runner = runner,
            jsonExtractor = extractor,
            jsonParser = ytDlpParser,
            fileFinder = SingleFileByPrefixFinder(),
            toolId = ToolId.YT_DLP,
        )

    fun galleryDl(cmd: CommandBuilder) =
        NoMetadataCliTool(
            props = props,
            pathGenerator = GalleryDlPathGenerator(props),
            cmdBuilder = cmd,
            runner = runner,
            fileFinder = DirectoryFilesByPrefixFinder(),
            toolId = ToolId.GALLERY_DL,
        )
}

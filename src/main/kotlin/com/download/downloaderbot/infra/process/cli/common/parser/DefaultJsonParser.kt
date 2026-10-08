package com.download.downloaderbot.infra.process.cli.common.parser

import com.download.downloaderbot.core.downloader.MalformedJsonException
import com.download.downloaderbot.infra.process.cli.api.interfaces.JsonParser
import com.download.downloaderbot.infra.process.cli.common.utils.preview
import tools.jackson.core.exc.StreamReadException
import tools.jackson.core.type.TypeReference
import tools.jackson.databind.DatabindException
import tools.jackson.databind.json.JsonMapper

class DefaultJsonParser<T>(
    val mapper: JsonMapper,
    val typeRef: TypeReference<T>,
) : JsonParser<T> {
    override suspend fun parse(json: String): T =
        try {
            mapper.readValue(json, typeRef)
        } catch (e: StreamReadException) {
            throw MalformedJsonException("Invalid JSON syntax", json.preview(), e)
        } catch (e: DatabindException) {
            throw MalformedJsonException("JSON does not match expected shape", json.preview(), e)
        }
}

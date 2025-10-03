package com.download.downloaderbot.infra.process.cli.common.parser

import com.download.downloaderbot.core.downloader.MalformedJsonException
import com.download.downloaderbot.infra.process.cli.api.interfaces.JsonParser
import com.download.downloaderbot.infra.process.cli.common.utils.preview
import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.ObjectMapper

class DefaultJsonParser<T>(
    val mapper: ObjectMapper,
    val typeRef: TypeReference<T>,
) : JsonParser<T> {
    override suspend fun parse(json: String): T =
        try {
            mapper.readValue(json, typeRef)
        } catch (e: JsonParseException) {
            throw MalformedJsonException("Invalid JSON syntax", json.preview(), e)
        } catch (e: JsonMappingException) {
            throw MalformedJsonException("JSON does not match expected shape", json.preview(), e)
        }
}

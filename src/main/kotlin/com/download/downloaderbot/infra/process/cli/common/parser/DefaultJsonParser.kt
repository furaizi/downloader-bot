package com.download.downloaderbot.infra.process.cli.common.parser

import com.download.downloaderbot.infra.process.cli.api.interfaces.JsonParser
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper

class DefaultJsonParser<T>(
    val mapper: ObjectMapper,
    val typeRef: TypeReference<T>,
) : JsonParser<T> {
    override suspend fun parse(json: String): T =
        try {
            mapper.readValue(json, typeRef)
        } catch (e: Exception) {
            throw RuntimeException("Failed to parse JSON: $json", e)
        }
}

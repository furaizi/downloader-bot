package com.download.downloaderbot.infra.config

import com.download.downloaderbot.infra.process.cli.api.interfaces.JsonParser
import com.download.downloaderbot.infra.process.cli.common.parser.DefaultJsonParser
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper

inline fun <reified T> jsonParser(mapper: ObjectMapper): JsonParser<T> = DefaultJsonParser(mapper, object : TypeReference<T>() {})

fun String.containsAll(vararg parts: String): Boolean = parts.all(this::contains)

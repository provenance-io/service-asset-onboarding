package tech.figure.asset.extensions

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.figure.data.json.JodaMoneyModule
import com.hubspot.jackson.datatype.protobuf.ProtobufModule
import java.util.*

fun ObjectMapper.configureFigureTech(): ObjectMapper = registerKotlinModule()
    .registerModule(JavaTimeModule())
    .registerModule(JodaMoneyModule())
    .registerModule(ProtobufModule())
    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    .setSerializationInclusion(JsonInclude.Include.NON_NULL)
    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

fun tech.figure.util.UUID.toUUID(): UUID = UUID.fromString(value)

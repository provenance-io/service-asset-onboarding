package tech.figure.asset.frameworks.web.misc

import com.figure.proto.extensions.toUuid
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.awaitBodyOrNull
import java.util.UUID

val ServerRequest.ipAddress: String
    get() = headers().firstHeader("x-real-ip") ?: remoteAddress().get().address.toString()

fun ServerRequest.uuidPathVariable(name: String = "uuid"): UUID =
    kotlin.runCatching {
        pathVariable(name).toUuid()
    }.getOrElse { throw IllegalArgumentException("Failed to parse uuid path variable '$name'", it) }

suspend inline fun <reified T> ServerRequest.requireBody(): T =
    if (T::class == Unit::class) {
        Unit as T
    } else {
        requireNotNull(awaitBodyOrNull()) { "Failed to parse request body of type ${T::class}" }
    }

package tech.figure.asset.frameworks.web.misc

import mu.KotlinLogging
import org.springframework.web.reactive.function.server.ServerResponse
import tech.figure.asset.frameworks.web.ErrorResponses
import tech.figure.asset.frameworks.web.SuccessResponses

private val log = KotlinLogging.logger {}

suspend fun Result<Any>.foldToServerResponse(): ServerResponse =
    fold(
        onSuccess = { if (it is Unit) SuccessResponses.ok() else SuccessResponses.ok(it) },
        onFailure = {
            log.warn(it) { "returning error response for ${it::class}" }
            ErrorResponses.defaultForType(it)
        }
    )

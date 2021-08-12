package tech.figure.asset.frameworks.web

import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.bodyValueAndAwait
import org.springframework.web.reactive.function.server.buildAndAwait

object SuccessResponses {

    suspend fun ok() = ServerResponse.ok().buildAndAwait()

    suspend fun ok(body: Any) = ServerResponse.ok().bodyValueAndAwait(body)
}

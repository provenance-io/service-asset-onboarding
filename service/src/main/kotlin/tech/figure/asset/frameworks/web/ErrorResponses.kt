package tech.figure.asset.frameworks.web

import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.bodyValueAndAwait
import org.springframework.web.reactive.function.server.buildAndAwait
import tech.figure.asset.domain.usecase.common.errors.ApiError
import tech.figure.asset.domain.usecase.common.errors.NotFoundError

object ErrorResponses {
    suspend fun status(status: Int) = ServerResponse.status(status).buildAndAwait()

    suspend fun badRequest(cause: Throwable) = ServerResponse.badRequest()
        .bodyValueAndAwait(cause.localizedMessage)

    suspend fun notFound(cause: Throwable) = ServerResponse.status(HttpStatus.NOT_FOUND)
        .bodyValueAndAwait(cause.localizedMessage)

    suspend fun internalError(cause: Throwable) = ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .bodyValueAndAwait(cause.localizedMessage)

    suspend fun defaultForType(cause: Throwable) = when (cause) {
        is NotFoundError -> notFound(cause)
        is IllegalArgumentException, is ApiError -> badRequest(cause)
        else -> internalError(cause)
    }
}

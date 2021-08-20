package tech.figure.asset.util.web

import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import java.util.UUID

fun <S : WebTestClient.RequestHeadersSpec<S>> WebTestClient.RequestHeadersSpec<S>.acceptJson(): S =
    accept(MediaType.APPLICATION_JSON)
// fun WebTestClient.RequestHeadersSpec<>.acceptJson() =
//    accept(MediaType.APPLICATION_JSON)

fun WebTestClient.RequestBodySpec.contentTypeJson() = contentType(MediaType.APPLICATION_JSON)

fun <S : WebTestClient.RequestHeadersSpec<S>> WebTestClient.RequestHeadersSpec<S>.applyFigureHeaders(
    uuid: UUID = UUID.randomUUID(),
): S = headers {
    it["x-uuid"] = "$uuid"
}

package tech.figure.asset.util.web

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import tech.figure.asset.frameworks.web.roles.Roles
import java.util.UUID

fun <S : WebTestClient.RequestHeadersSpec<S>> WebTestClient.RequestHeadersSpec<S>.acceptJson(): S =
    accept(MediaType.APPLICATION_JSON)
// fun WebTestClient.RequestHeadersSpec<>.acceptJson() =
//    accept(MediaType.APPLICATION_JSON)

fun WebTestClient.RequestBodySpec.contentTypeJson() = contentType(MediaType.APPLICATION_JSON)

fun <S : WebTestClient.RequestHeadersSpec<S>> WebTestClient.RequestHeadersSpec<S>.applyFigureHeaders(
    uuid: UUID = UUID.randomUUID(),
    roles: Map<String, List<Roles>> = emptyMap(),
): S = headers {
    it["x-uuid"] = "$uuid"
    it["x-roles"] = jacksonObjectMapper().writeValueAsString(roles)
}

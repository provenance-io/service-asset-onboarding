package tech.figure.asset.frameworks.web.roles

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import mu.KotlinLogging
import org.springframework.web.reactive.function.server.ServerRequest

/**
 * Parses the list of [Roles] from the request header. Roles should appear in the header under key 'x-roles'.
 */
@Suppress("TooGenericExceptionCaught")
fun ServerRequest.roles(): List<Roles> {
    val text = requireNotNull(headers().firstHeader("x-roles"))
    val nodes = jacksonObjectMapper().readTree(text)
    return try {
        nodes["service-asset-onboarding"].map { Roles.valueOf(it.asText()) }
    } catch (err: Throwable) {
        KotlinLogging.logger("ROLES").error(err) { "error parsing roles header" }
        emptyList()
    }
}

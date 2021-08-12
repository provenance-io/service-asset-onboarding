package tech.figure.asset.frameworks.web.roles

import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.server.CoRouterFunctionDsl
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.bodyValueAndAwait
import org.springframework.web.reactive.function.server.coRouter

/**
 * Variadic argument version of [secured].
 */
fun CoRouterFunctionDsl.secured(vararg requiredRoles: Roles) = secured(requiredRoles.toList())

/**
 * Secure access to the current router scope with the given list of [Roles].
 */
fun CoRouterFunctionDsl.secured(requiredRoles: List<Roles>) {
    require(requiredRoles.isNotEmpty()) { "must specify at least one role" }
    filter { request, handler ->
        if (request.roles().any { it in requiredRoles }) {
            handler(request)
        } else {
            ServerResponse.status(HttpStatus.UNAUTHORIZED)
                .contentType(MediaType.TEXT_PLAIN)
                .bodyValueAndAwait("Resource requires at least one of: ${requiredRoles.joinToString()}")
        }
    }
}

/**
 * Variadic argument version of [securedCoRouter].
 */
fun securedCoRouter(vararg roles: Roles, routes: (CoRouterFunctionDsl.() -> Unit)) =
    securedCoRouter(roles.toList(), routes)

/**
 * Creates a router scope secured with the given list of [Roles].
 */
fun securedCoRouter(roles: List<Roles>, routes: (CoRouterFunctionDsl.() -> Unit)) = coRouter {
    secured(roles)
    apply(routes)
}

package tech.figure.asset.frameworks.web.logging

import mu.KLogger
import org.springframework.web.reactive.function.server.CoRouterFunctionDsl

fun CoRouterFunctionDsl.logExchange(log: KLogger) {
    before { req ->
        log.info { "<-- ${req.method()} ${req.path()}" }
        req
    }
    after { req, resp ->
        log.info { "--> ${resp.statusCode()} | ${req.method()} ${req.path()}" }
        resp
    }
}

package io.provenance.asset.config

import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.context.request.WebRequest
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler
import io.provenance.asset.exceptions.MissingPublicKeyException

@ControllerAdvice(basePackages = ["io.provenance.asset.web"])
@Order(Ordered.HIGHEST_PRECEDENCE)
class GlobalControllerAdvice : ResponseEntityExceptionHandler() {

    @ExceptionHandler(MissingPublicKeyException::class)
    fun handleMissingPublicKey(exception: MissingPublicKeyException, request: WebRequest): ResponseEntity<ErrorMessage> {
        logger.warn("Missing public key: ${request.contextPath}")
        return ResponseEntity(ErrorMessage(listOf(exception.message ?: "Missing public key")), HttpStatus.FORBIDDEN)
    }

    /**
     * Catch-all for everything else
     */
    @ExceptionHandler(Exception::class)
    @ResponseBody
    fun handleAny(exception: Exception, request: WebRequest): ResponseEntity<String> {
        val message = exception.message ?: exception.javaClass.simpleName
        logger.error("${request.contextPath} failed with error $message (catch-all handler)", exception)
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(message)
    }

}

data class ErrorMessage(val errors: List<String>)

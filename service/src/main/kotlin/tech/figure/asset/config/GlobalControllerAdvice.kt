package tech.figure.asset.config

import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.context.request.WebRequest
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler
import tech.figure.asset.exceptions.MissingPublicKeyException

@ControllerAdvice(basePackages = ["tech.figure.asset.web"])
@Order(Ordered.HIGHEST_PRECEDENCE)
class GlobalControllerAdvice : ResponseEntityExceptionHandler() {

    @ExceptionHandler(MissingPublicKeyException::class)
    fun handleMissingPublicKey(exception: MissingPublicKeyException, request: WebRequest): ResponseEntity<ErrorMessage> {
        logger.warn("Missing public key: ${request.contextPath}")
        return ResponseEntity(ErrorMessage(listOf(exception.message ?: "Missing public key")), HttpStatus.FORBIDDEN)
    }

}

data class ErrorMessage(val errors: List<String>)

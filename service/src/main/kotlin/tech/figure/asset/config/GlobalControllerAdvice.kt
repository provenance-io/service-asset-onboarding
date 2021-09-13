package tech.figure.asset.config

import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.web.bind.annotation.ControllerAdvice
// import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler

@ControllerAdvice(basePackages = ["tech.figure.asset.web"])
@Order(Ordered.HIGHEST_PRECEDENCE)
class GlobalControllerAdvice /*: ResponseEntityExceptionHandler()*/ {

    // TODO

}

package tech.figure.asset

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
import org.springframework.boot.runApplication
import springfox.documentation.swagger2.annotations.EnableSwagger2
import tech.figure.asset.extensions.configureProvenance

@EnableSwagger2
@SpringBootApplication(
    exclude = [
        DataSourceAutoConfiguration::class
    ]
)
class Application

@Suppress("SpreadOperator")
fun main(args: Array<String>) {
    runApplication<Application>(*args)
}

val OBJECT_MAPPER = ObjectMapper().configureProvenance()

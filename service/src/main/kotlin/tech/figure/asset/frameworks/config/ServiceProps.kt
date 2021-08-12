package tech.figure.asset.frameworks.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.validation.annotation.Validated

@ConstructorBinding
@ConfigurationProperties(prefix = "service")
@Validated
data class ServiceProps(
    val name: String,
    val environment: String,
) {

    fun isProd() = environment == "production"

    override fun toString(): String {
        return """Service Properties:
            | name: $name
            | environment: $environment
        """.trimMargin()
    }
}

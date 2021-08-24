package tech.figure.asset.config

import org.springframework.boot.context.properties.ConfigurationProperties
import javax.validation.constraints.NotNull

@ConfigurationProperties(prefix = "object-store")
class ObjectStoreProperties : LoggableProperties() {

    @NotNull
    lateinit var url: String

    @NotNull
    var timeoutMs: Long = 60000L

}

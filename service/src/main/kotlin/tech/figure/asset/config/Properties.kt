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

@ConfigurationProperties(prefix = "provenance")
class ProvenanceProperties : LoggableProperties() {

    @NotNull
    var isMainnet: Boolean = false

}

@ConfigurationProperties(prefix = "asset-spec")
class AssetSpecificationProperties : LoggableProperties() {

    @NotNull
    lateinit var contractSpecId: String

    @NotNull
    lateinit var scopeSpecId: String

}

@ConfigurationProperties(prefix = "docket")
class DocketProperties : LoggableProperties() {

    @NotNull
    lateinit var host: String

    @NotNull
    lateinit var protocols: Set<String>

}

@ConfigurationProperties(prefix = "service-keys")
class ServiceKeysProperties : LoggableProperties() {

    @NotNull
    lateinit var assetManager: String

}

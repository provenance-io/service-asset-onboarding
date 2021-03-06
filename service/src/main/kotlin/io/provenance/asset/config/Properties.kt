package io.provenance.asset.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.net.URI
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
    var mainnet: Boolean = false

    @NotNull
    lateinit var chainId: String

    @NotNull
    lateinit var channelUri: URI

    @NotNull
    lateinit var assetClassificationContractName: String
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

    @NotNull
    lateinit var contactName: String

    @NotNull
    lateinit var contactUrl: String

    @NotNull
    lateinit var apiTitle: String

}

@ConfigurationProperties(prefix = "service-keys")
class ServiceKeysProperties : LoggableProperties() {

    @NotNull
    lateinit var assetManager: String

}

@ConfigurationProperties(prefix = "cors")
class CorsProperties : LoggableProperties() {

    var allowedOrigins: List<String> = emptyList()
    var allowedHeaders: List<String> = emptyList()
    var allowedMethods: List<String> = emptyList()

}
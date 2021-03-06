package io.provenance.asset.config

import com.fasterxml.jackson.databind.ObjectMapper
import io.provenance.classification.asset.client.client.base.ACClient
import io.provenance.classification.asset.client.client.base.ContractIdentifier
import io.provenance.client.grpc.GasEstimationMethod
import io.provenance.client.grpc.PbClient
import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.info.BuildProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.http.MediaType
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import org.springframework.web.filter.CorsFilter
import springfox.documentation.builders.RequestHandlerSelectors
import springfox.documentation.service.ApiInfo
import springfox.documentation.service.Contact
import springfox.documentation.spi.DocumentationType
import springfox.documentation.spring.web.plugins.Docket
import io.provenance.asset.OBJECT_MAPPER
import io.provenance.asset.extensions.info
import io.provenance.asset.services.AssetOnboardService
import java.lang.management.ManagementFactory


@Configuration
@EnableConfigurationProperties(
    value = [
        ObjectStoreProperties::class,
        ProvenanceProperties::class,
        AssetSpecificationProperties::class,
        DocketProperties::class,
        ServiceKeysProperties::class,
        CorsProperties::class
    ]
)
@Import(BuildProperties::class)
class AppConfig(
    buildProperties: BuildProperties,
    objectStoreProperties: ObjectStoreProperties,
    provenanceProperties: ProvenanceProperties,
    assetSpecificationProperties: AssetSpecificationProperties,
    docketProperties: DocketProperties,
    serviceKeysProperties: ServiceKeysProperties,
    corsProperties: CorsProperties
) {

    private var logger = LoggerFactory.getLogger(AppConfig::class.java)

    init {
        buildProperties.sortedBy { it.key }.forEach {
            logger.info("Build property: ${it.key} - ${it.value}")
        }

        logger.info(objectStoreProperties.toLogMessages())
        logger.info(provenanceProperties.toLogMessages())
        logger.info(assetSpecificationProperties.toLogMessages())
        logger.info(docketProperties.toLogMessages())
        logger.info(serviceKeysProperties.toLogMessages())
        logger.info(corsProperties.toLogMessages())

        ManagementFactory.getRuntimeMXBean().inputArguments.map {
            logger.info("JVM arg: $it")
        }
    }

    @Primary
    @Bean
    fun mapper(): ObjectMapper = OBJECT_MAPPER

    @Bean
    fun pbClient(
        provenanceProperties: ProvenanceProperties
    ): PbClient = PbClient(
        chainId = provenanceProperties.chainId,
        channelUri = provenanceProperties.channelUri,
        gasEstimationMethod = GasEstimationMethod.MSG_FEE_CALCULATION,
    )

    @Bean
    fun aCClient(
        provenanceProperties: ProvenanceProperties,
        pbClient: PbClient
    ): ACClient = ACClient.getDefault(
        contractIdentifier = ContractIdentifier.Name(provenanceProperties.assetClassificationContractName),
        pbClient = pbClient,
        objectMapper = OBJECT_MAPPER,
    )

    @Bean
    fun assetOnboardService(
        acClient: ACClient,
        pbClient: PbClient,
        objectStoreProperties: ObjectStoreProperties,
        assetSpecificationProperties: AssetSpecificationProperties,
    ) = AssetOnboardService(acClient, pbClient, objectStoreProperties, assetSpecificationProperties)

    @Bean
    fun api(docketProperties: DocketProperties): Docket {

        val contact = Contact(docketProperties.contactName, docketProperties.contactUrl, null)

        val apiInfo = ApiInfo(
            docketProperties.apiTitle,
            "",
            "1.0",
            "",
            contact,
            "",
            "",
            listOf()
        )

        return Docket(DocumentationType.OAS_30)
            .apiInfo(apiInfo)
            .host(docketProperties.host)
            .consumes(setOf(MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_OCTET_STREAM_VALUE, MediaType.MULTIPART_FORM_DATA_VALUE))
            .produces(setOf(MediaType.APPLICATION_JSON_VALUE))
            .protocols(docketProperties.protocols)
            .forCodeGeneration(true)
            .securitySchemes(listOf(
                    // don't use apikey until we have the ability to bill someone
                    // ApiKey("Token Access", "apikey", "header")
            ))
            .select()
            .apis(RequestHandlerSelectors.basePackage("io.provenance.asset.web"))
            .build()
    }

    @Bean
    fun corsFilter(props: CorsProperties): CorsFilter = CorsFilter(UrlBasedCorsConfigurationSource().also {
        val apiConfig = CorsConfiguration().also {
            it.setAllowCredentials(false)
            props.allowedOrigins.forEach { origin ->
                it.addAllowedOrigin(origin)
            }
            props.allowedHeaders.forEach { header ->
                it.addAllowedHeader(header)
            }
            props.allowedMethods.forEach { method ->
                it.addAllowedMethod(method)
            }
        }

        it.registerCorsConfiguration("/api/**", apiConfig)
    })

}

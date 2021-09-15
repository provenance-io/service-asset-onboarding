package tech.figure.asset.config

import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.info.BuildProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import springfox.documentation.builders.RequestHandlerSelectors
import springfox.documentation.service.ApiInfo
import springfox.documentation.service.ApiKey
import springfox.documentation.service.Contact
import springfox.documentation.spi.DocumentationType
import springfox.documentation.spring.web.plugins.Docket
import springfox.documentation.swagger.web.*
import tech.figure.asset.extensions.info
import tech.figure.asset.services.AssetOnboardService
import java.lang.management.ManagementFactory


@Configuration
@EnableConfigurationProperties(
    value = [
        ObjectStoreProperties::class,
        DocketProperties::class
    ]
)
@Import(BuildProperties::class)
class AppConfig(
    buildProperties: BuildProperties,
    objectStoreProperties: ObjectStoreProperties
) {

    private var logger = LoggerFactory.getLogger(AppConfig::class.java)

    init {
        buildProperties.sortedBy { it.key }.forEach {
            logger.info("Build property: ${it.key} - ${it.value}")
        }

        logger.info(objectStoreProperties.toLogMessages())

        ManagementFactory.getRuntimeMXBean().inputArguments.map {
            logger.info("JVM arg: $it")
        }
    }

    @Bean
    fun assetOnboardService(objectStoreProperties: ObjectStoreProperties) = AssetOnboardService(objectStoreProperties)


    @Bean
    fun api(docketProperties: DocketProperties): Docket {

        val contact = Contact("Figure Technologies", "https://figure.tech", null)

        val apiInfo = ApiInfo(
            "Figure Tech Asset Onboarding API",
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
            .consumes(setOf("application/json"))
            .produces(setOf("application/json"))
            .protocols(docketProperties.protocols)
            .forCodeGeneration(true)
            .securitySchemes(listOf(
                    // don't use apikey until we have the ability to bill someone
                    // ApiKey("Token Access", "apikey", "header")
            ))
            .select()
            .apis(RequestHandlerSelectors.basePackage("tech.figure.asset.web"))
            .build()
    }

    @Bean
    fun uiConfig(): UiConfiguration {
        return UiConfigurationBuilder.builder()
            .deepLinking(true)
            .displayOperationId(false)
            .defaultModelsExpandDepth(1)
            .defaultModelExpandDepth(1)
            .defaultModelRendering(ModelRendering.EXAMPLE)
            .displayRequestDuration(false)
            .docExpansion(DocExpansion.FULL)
            .filter(false)
            .maxDisplayedTags(null)
            .operationsSorter(OperationsSorter.ALPHA)
            .showExtensions(false)
            .showCommonExtensions(false)
            .tagsSorter(TagsSorter.ALPHA)
            .validatorUrl(null)
            .build()
    }

}

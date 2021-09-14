package tech.figure.asset.config

import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.info.BuildProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import springfox.documentation.builders.RequestHandlerSelectors
import springfox.documentation.service.ApiInfo
import springfox.documentation.service.Contact
import springfox.documentation.service.Parameter
import springfox.documentation.spi.DocumentationType
import springfox.documentation.spring.web.plugins.Docket
import tech.figure.asset.extensions.info
import tech.figure.asset.services.AssetOnboardService
import java.lang.management.ManagementFactory

@Configuration
@EnableConfigurationProperties(
    value = [
        ObjectStoreProperties::class
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
    fun api(): Docket {

        val contact = Contact("Figure", null, null)

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

        val params = mutableListOf<Parameter>(
            // todo x-uuid
//                ParameterBuilder()
//                        .name("x-uuid")
//                        .description("Provenance admin ID")
//                        .modelRef(ModelRef("uuid"))
//                        .required(true)
//                        .parameterType("header")
//                        .build()

//                ParameterBuilder()
//                        .name("apikey")
//                        .description("apikey")
//                        .modelRef(ModelRef("string"))
//                        .required(true)
//                        .parameterType("header")
//                        .build()

        )

        return Docket(DocumentationType.SWAGGER_2)
            .apiInfo(apiInfo)
            .host("localhost:8080")
            .consumes(setOf("application/json"))
            .produces(setOf("application/json"))
            .protocols(setOf("http", "https"))
            .forCodeGeneration(true)
            .globalOperationParameters(params)
            .select()
            .apis(RequestHandlerSelectors.basePackage("tech.figure.asset.web"))
            .build()
    }

}

package tech.figure.asset.config

import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.info.BuildProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
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

}

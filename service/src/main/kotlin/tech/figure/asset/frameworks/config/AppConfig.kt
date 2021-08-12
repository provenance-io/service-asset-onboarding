package tech.figure.asset.frameworks.config

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.figure.data.OBJECT_MAPPER
import com.figure.data.json.configureFigure
import com.hubspot.jackson.datatype.protobuf.ProtobufModule
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary

@Configuration
@EnableConfigurationProperties(
    value = [
        ServiceProps::class,
    ]
)
class AppConfig {
    @Primary
    @Bean
    fun objectMapper() = OBJECT_MAPPER.configureFigure()
        .registerModule(ProtobufModule())
        .registerModule(JavaTimeModule())
}

package tech.figure.asset.frameworks.web.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.figure.spring.webflux.security.ReactiveIdentitySecurityConfigurer
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.web.server.SecurityWebFilterChain
import tech.figure.asset.frameworks.config.ServiceProps
import tech.figure.asset.frameworks.web.Routes

@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
@EnableConfigurationProperties(value = [ServiceProps::class])
class SecurityConfig(
    serviceProps: ServiceProps,
    objectMapper: ObjectMapper
) {

    private val configurer = ReactiveIdentitySecurityConfigurer(serviceProps.name, objectMapper, "/")

    @Bean
    fun securityWebFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
        http.authorizeExchange {
            it.pathMatchers("${Routes.MANAGE_BASE}/**").permitAll()
        }
        return configurer.configure(
            http,
            "${Routes.SECURE_BASE}/**",
        )
    }
}

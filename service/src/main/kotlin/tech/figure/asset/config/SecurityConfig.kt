package tech.figure.asset.config

import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.builders.WebSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.config.http.SessionCreationPolicy

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(securedEnabled = true)
class SecurityConfig : WebSecurityConfigurerAdapter() {

    @Throws(Exception::class)
    override fun configure(http: HttpSecurity) {
        http.sessionManagement()
            .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .csrf().disable()
            .headers().frameOptions().disable()

        http.authorizeRequests()
            .antMatchers("/swagger*/**").permitAll()
            .antMatchers("/webjars/**").permitAll()
            .antMatchers("/v3/api-docs*").permitAll()
            .antMatchers("/v2/api-docs*").permitAll()
            .antMatchers("/api/**").permitAll()

    }

    @Throws(Exception::class)
    override fun configure(webSecurity: WebSecurity) {
        webSecurity
            .ignoring()
            .antMatchers("/actuator/**")
            .antMatchers("/swagger*/**").permitAll()
            .antMatchers("/webjars/**").permitAll()
            .antMatchers("/v3/api-docs*").permitAll()
            .antMatchers("/v2/api-docs*").permitAll()
            .antMatchers("/api/**").permitAll()
    }

}

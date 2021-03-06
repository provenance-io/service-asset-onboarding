package io.provenance.asset.config

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

//        http.authorizeRequests()
//            .antMatchers("/api/**").permitAll()
//            .anyRequest()
//            .authenticated()

        http.csrf()
            .disable()
    }

    @Throws(Exception::class)
    override fun configure(webSecurity: WebSecurity) {
        webSecurity
            .ignoring()
            .antMatchers("/actuator/**")
            .antMatchers("/swagger*/**")
            .antMatchers("/webjars/**")
            .antMatchers("/v3/api-docs*")
            .antMatchers("/v2/api-docs*")
            .antMatchers("/api/**")
    }

}

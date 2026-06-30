package com.mcpratapp.config

import com.mcpratapp.security.JwtFilter
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
@EnableWebSecurity
class SecurityConfig(private val jwtFilter: JwtFilter) {

    private val logger = LoggerFactory.getLogger(SecurityConfig::class.java)

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .cors { it.configurationSource(corsConfigurationSource()) }
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { authorize ->
                logger.info("Security config")
                authorize
                    .requestMatchers("/api/v1/auth/**").permitAll()
                    .requestMatchers("/api/v1/products", "/api/v1/products/**").hasAnyRole("VENDOR", "ADMIN")
                    .requestMatchers("/api/v1/product-vendors", "/api/v1/product-vendors/**").hasAnyRole("VENDOR", "ADMIN")
                    .requestMatchers("/api/v1/users", "/api/v1/users/**").hasRole("ADMIN")
                    .requestMatchers("/api/v1/clients", "/api/v1/clients/**").hasAnyRole("ADMIN", "VENDOR")
                    .requestMatchers("/api/v1/orders", "/api/v1/orders/**").hasAnyRole("ADMIN", "VENDOR")
                    .requestMatchers("/api/v1/payments", "/api/v1/payments/**").hasAnyRole("ADMIN", "VENDOR")
                    .requestMatchers("/error").permitAll()  // Permitir endpoint de erro
                    .requestMatchers("/actuator/**").permitAll()  // Permitir health check
                    .anyRequest().authenticated()
            }
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter::class.java)
            .httpBasic { it.disable() }

        return http.build()
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder()
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val config = CorsConfiguration().apply {
            allowedOrigins = listOf("http://localhost:5173")
            allowedMethods = listOf("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
            allowedHeaders = listOf("*")
            exposedHeaders = listOf("Authorization")
            allowCredentials = true
            maxAge = 3600
        }

        logger.info("CORS configurado para: http://localhost:5173")

       val source = UrlBasedCorsConfigurationSource()
       source.registerCorsConfiguration("/**", config)
       return source
    }
}

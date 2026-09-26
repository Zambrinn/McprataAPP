package com.mcpratapp.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {
    val securitySchemeName = "bearerAuth"
    @Bean
    fun customOpenAPI(): OpenAPI {
        return OpenAPI()
            .info(
                Info()
                    .title("MCPrataAPP REST Documentation")
                    .description("REST API OpenAPI documentation")
                    .version("v1.0.0")
                    .contact(
                        Contact()
                            .name("Thiago Zambrin Filho")
                            .email("thiago.zambrin@edu.unifil.br")
                    )
            )

            .addSecurityItem(SecurityRequirement().addList(securitySchemeName))
            .components(
                Components()
                    .addSecuritySchemes(
                        securitySchemeName,
                        SecurityScheme()
                            .name(securitySchemeName)
                            .type(SecurityScheme.Type.HTTP)
                            .scheme("bearer")
                            .bearerFormat("JWT")
                            .description("Insira o token JWT retornado no login (sem a palavra Bearer)")
                    )
            )
    }
}
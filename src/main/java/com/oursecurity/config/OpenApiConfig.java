package com.oursecurity.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI ourSecurityOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("OurSecurity API")
                        .version("v1")
                        .description("""
                                REST API for the OurSecurity gated community app.
                                Log in with POST /api/auth/login, then send the token as `Authorization: Bearer <token>`.
                                Roles: RESIDENT, GUARD, COMMITTEE. Errors are returned as {"message": "..."}."""))
                .components(new Components().addSecuritySchemes("bearerAuth",
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}

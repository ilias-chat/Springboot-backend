package com.dwsc.backend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 3 — same paths as TRWM-backend: {@code /api/docs} (UI), {@code /api/docs.json} (spec).
 */
@Configuration
public class OpenApiConfig {

    public static final String BEARER_AUTH = "bearerAuth";

    @Bean
    public OpenAPI dwscOpenApi() {
        return new OpenAPI()
                .info(
                        new Info()
                                .title("DWSC Backend API")
                                .version("0.0.1-SNAPSHOT")
                                .description(
                                        """
                                        REST API for DWSC (Spring Boot + PostgreSQL). \
                                        Secured user routes expect a **Firebase ID token** in \
                                        `Authorization: Bearer <token>`.

                                        In Swagger UI **Authorize**, paste **only** the JWT (no `Bearer ` prefix). \
                                        The token is validated when you **Execute** a request."""))
                .components(
                        new Components()
                                .addSecuritySchemes(
                                        BEARER_AUTH,
                                        new SecurityScheme()
                                                .type(SecurityScheme.Type.HTTP)
                                                .scheme("bearer")
                                                .bearerFormat("JWT")
                                                .description(
                                                        "Firebase ID token from the client after sign-in.")));
    }
}

package com.dwsc.comment.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    public static final String BEARER_AUTH = "bearerAuth";

    @Bean
    public OpenAPI dwscCommentOpenApi() {
        return new OpenAPI()
                .info(
                        new Info()
                                .title("DWSC Comment Service API")
                                .version("0.0.1-SNAPSHOT")
                                .description("Comment microservice for DWSC (Spring Boot + PostgreSQL)."))
                .components(
                        new Components()
                                .addSecuritySchemes(
                                        BEARER_AUTH,
                                        new SecurityScheme()
                                                .type(SecurityScheme.Type.HTTP)
                                                .scheme("bearer")
                                                .bearerFormat("JWT")
                                                .description("Firebase ID token from the client after sign-in.")));
    }
}


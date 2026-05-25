package com.dwsc.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Same origins idea as TRWM-backend {@code app.js} {@code parseCorsOrigins()}: local dev hosts +
 * {@code FRONTEND_ORIGIN} / {@code CORS_ORIGINS} (comma-separated).
 */
@Configuration
public class CorsWebConfig implements WebMvcConfigurer {

    @Value("${FRONTEND_ORIGIN:}")
    private String frontendOrigin;

    @Value("${CORS_ORIGINS:}")
    private String corsOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        Set<String> origins = new LinkedHashSet<>();
        origins.addAll(
                Arrays.asList(
                        "http://localhost:4200",
                        "http://127.0.0.1:4200",
                        "http://localhost:8100",
                        "http://127.0.0.1:8100",
                        "ionic://localhost",
                        "capacitor://localhost",
                        "https://localhost"));
        Stream.of(frontendOrigin, corsOrigins)
                .filter(s -> s != null && !s.isBlank())
                .flatMap(s -> Arrays.stream(s.split(",")))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .forEach(origins::add);

        registry.addMapping("/**")
                .allowedOriginPatterns(origins.toArray(String[]::new))
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}

package com.dwsc.backend.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * Matches TRWM-backend {@code formatUser()} JSON: {@code id}, {@code firebaseUID}, ISO dates.
 */
@Schema(name = "User")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserResponse(
        @Schema(description = "PostgreSQL user id") String id,
        @JsonProperty("firebaseUID") String firebaseUid,
        @Schema(format = "email") String email,
        @Schema(allowableValues = {"user", "admin"}) String role,
        String name,
        @Schema(description = "Image URL; may be empty string") String avatar,
        @Schema(format = "date-time") Instant createdAt,
        @Schema(format = "date-time") Instant updatedAt
) {}

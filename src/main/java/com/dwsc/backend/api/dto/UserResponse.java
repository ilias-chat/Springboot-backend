package com.dwsc.backend.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

/**
 * Matches TRWM-backend {@code formatUser()} JSON: {@code id}, {@code firebaseUID}, ISO dates.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserResponse(
        String id,
        @JsonProperty("firebaseUID") String firebaseUid,
        String email,
        String role,
        String name,
        String avatar,
        Instant createdAt,
        Instant updatedAt
) {}

package com.dwsc.backend.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** Matches Node `{ "error": "..." }` responses. */
@Schema(name = "Error", requiredProperties = "error")
public record ErrorResponse(
        @Schema(description = "Human-readable message") String error) {}

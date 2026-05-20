package com.dwsc.backend.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "UpdateProfileBody", description = "At least one field required.")
public record UpdateProfileBody(
        @Schema(description = "Display name") String name,
        @Schema(description = "Profile image URL; empty string clears avatar") String avatar) {}

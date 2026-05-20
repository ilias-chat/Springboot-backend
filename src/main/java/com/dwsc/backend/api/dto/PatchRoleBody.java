package com.dwsc.backend.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(name = "PatchRoleBody", requiredProperties = "role")
public record PatchRoleBody(
        @Schema(allowableValues = {"user", "admin"}, requiredMode = Schema.RequiredMode.REQUIRED) String role) {}

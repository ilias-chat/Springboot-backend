package com.dwsc.backend.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "SyncUserBody", description = "Create or update profile after Firebase registration")
public record SyncUserBody(
        @Schema(description = "Must match the uid in the Firebase ID token.", requiredMode = Schema.RequiredMode.REQUIRED)
                @JsonProperty("firebaseUID")
                String firebaseUid,
        @Schema(format = "email", requiredMode = Schema.RequiredMode.REQUIRED) String email,
        @Schema(description = "Optional display name") String name,
        @Schema(description = "Optional profile image URL") String avatar) {}

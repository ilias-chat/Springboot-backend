package com.dwsc.backend.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(name = "AddCommentBody")
public record AddCommentRequest(
        @Schema(maxLength = 1000) String text,
        @Schema(minimum = "0", maximum = "5") Double rating,
        Double lat,
        Double lng) {}

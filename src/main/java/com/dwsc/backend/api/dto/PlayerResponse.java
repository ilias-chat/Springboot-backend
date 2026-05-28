package com.dwsc.backend.api.dto;

import com.dwsc.backend.model.GeoJsonPoint;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

/** Mongo-shaped player JSON for Ionic ({@code _id}, embedded {@code comments}). */
@Schema(name = "Player")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PlayerResponse(
        @JsonProperty("_id") String id,
        String name,
        String team,
        String league,
        String image,
        String position,
        JsonNode stats,
        String venueName,
        @Schema(format = "date-time") Instant registrationDate,
        Integer externalId,
        GeoJsonPoint location,
        List<PlayerCommentResponse> comments,
        @Schema(description = "Scout report count when comments are not embedded") Long reviewCount,
        @Schema(description = "Average scout rating when comments are not embedded") Double avgRating,
        @Schema(format = "date-time") Instant createdAt,
        @Schema(format = "date-time") Instant updatedAt) {}

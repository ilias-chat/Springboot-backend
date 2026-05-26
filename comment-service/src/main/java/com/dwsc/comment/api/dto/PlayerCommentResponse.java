package com.dwsc.comment.api.dto;

import com.dwsc.comment.model.GeoJsonPoint;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(name = "PlayerComment")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PlayerCommentResponse(
        @JsonProperty("_id") String id,
        String author,
        String authorName,
        String text,
        int rating,
        GeoJsonPoint location,
        Instant createdAt) {}


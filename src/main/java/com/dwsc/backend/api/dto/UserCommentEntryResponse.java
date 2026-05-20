package com.dwsc.backend.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(name = "UserCommentEntry")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserCommentEntryResponse(
        @JsonProperty("_id") String id,
        String text,
        int rating,
        String author,
        String authorName,
        Instant createdAt,
        UserCommentPlayerSummary player
) {}

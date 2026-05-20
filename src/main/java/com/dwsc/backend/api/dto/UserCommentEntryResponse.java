package com.dwsc.backend.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

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

package com.dwsc.backend.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "UserCommentPlayerSummary")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserCommentPlayerSummary(
        @JsonProperty("_id") String id,
        String name,
        String team,
        String league,
        String image
) {}

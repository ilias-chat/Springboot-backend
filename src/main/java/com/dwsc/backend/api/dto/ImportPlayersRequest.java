package com.dwsc.backend.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(name = "ImportPlayersBody")
public record ImportPlayersRequest(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Integer leagueId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Integer teamId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Integer season,
        List<Integer> externalIds) {}

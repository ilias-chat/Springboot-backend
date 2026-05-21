package com.dwsc.backend.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(name = "CreatePlayerBody")
public record CreatePlayerRequest(
        String name,
        @Schema(allowableValues = {"Attacker", "Midfielder", "Defender", "Goalkeeper"}) String position,
        Integer leagueId,
        Integer teamId,
        Integer season,
        Double lat,
        Double lng,
        String image) {}

package com.dwsc.backend.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(name = "UpdatePlayerBody")
public record UpdatePlayerRequest(
        String name,
        @Schema(allowableValues = {"Attacker", "Midfielder", "Defender", "Goalkeeper"}) String position,
        Double lat,
        Double lng,
        Integer leagueId,
        Integer teamId,
        Integer season,
        String image) {}

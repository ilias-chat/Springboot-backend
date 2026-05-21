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
        @Schema(
                        description =
                                "Device latitude (required when API-Football has no stadium coordinates). GeoJSON order: lng, lat on stored Point.")
                Double lat,
        @Schema(
                        description =
                                "Device longitude (required when API-Football has no stadium coordinates).")
                Double lng,
        String image) {}

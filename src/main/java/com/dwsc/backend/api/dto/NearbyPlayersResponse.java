package com.dwsc.backend.api.dto;

import com.dwsc.backend.model.GeoJsonPoint;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(name = "NearbyPlayersResponse")
public record NearbyPlayersResponse(List<PlayerResponse> players, List<StadiumSummary> stadiums) {

    @Schema(name = "StadiumSummary")
    public record StadiumSummary(String name, GeoJsonPoint location) {}
}
